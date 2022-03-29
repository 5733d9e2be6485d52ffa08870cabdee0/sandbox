package com.redhat.service.bridge.manager;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.bridge.manager.providers.ResourceNamesProvider;
import com.redhat.service.bridge.manager.workers.WorkManager;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class BridgesServiceImpl implements BridgesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgesServiceImpl.class);

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorService processorService;

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    ShardService shardService;

    @Inject
    WorkManager workManager;

    @Override
    public Bridge createBridge(String customerId, BridgeRequest bridgeRequest) {
        if (findByNameAndCustomerId(bridgeRequest.getName(), customerId) != null) {
            throw new AlreadyExistingItemException(String.format("Bridge with name '%s' already exists for customer with id '%s'", bridgeRequest.getName(), customerId));
        }

        Bridge bridge = doCreateBridge(customerId, bridgeRequest);

        LOGGER.info("Bridge with id '{}' has been created for customer '{}'", bridge.getId(), bridge.getCustomerId());

        workManager.schedule(bridge);

        return bridge;
    }

    @Transactional
    protected Bridge doCreateBridge(String customerId, BridgeRequest bridgeRequest) {
        if (bridgeDAO.findByNameAndCustomerId(bridgeRequest.getName(), customerId) != null) {
            throw new AlreadyExistingItemException(String.format("Bridge with name '%s' already exists for customer with id '%s'", bridgeRequest.getName(), customerId));
        }

        Bridge bridge = bridgeRequest.toEntity();
        bridge.setStatus(ManagedResourceStatus.ACCEPTED);
        bridge.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        bridge.setCustomerId(customerId);
        bridge.setShardId(shardService.getAssignedShardId(bridge.getId()));
        bridgeDAO.persist(bridge);

        return bridge;
    }

    @Transactional
    @Override
    public Bridge getBridge(String id) {
        Bridge b = bridgeDAO.findById(id);
        if (b == null) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' does not exist", id));
        }
        return b;
    }

    @Transactional
    public Bridge getReadyBridge(String idOrName, String customerId) {
        Bridge bridge = getBridgeByIdOrName(idOrName, customerId);
        if (ManagedResourceStatus.READY != bridge.getStatus()) {
            throw new BridgeLifecycleException(String.format("Bridge with id '%s' for customer '%s' is not in the '%s' state.", bridge.getId(), bridge.getCustomerId(), ManagedResourceStatus.READY));
        }
        return bridge;
    }

    @Override
    @Transactional
    public Bridge getBridgeByIdOrName(String idOrName, String customerId) {
        Bridge bridge = bridgeDAO.findByIdOrNameAndCustomerId(idOrName, customerId);
        if (bridge == null) {
            throw new ItemNotFoundException(String.format("Bridge with idOrName '%s' for customer '%s' does not exist", idOrName, customerId));
        }
        return bridge;
    }

    @Transactional
    public Bridge findByIdAndCustomerId(String id, String customerId) {
        Bridge bridge = bridgeDAO.findByIdAndCustomerId(id, customerId);
        if (bridge == null) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' for customer '%s' does not exist", id, customerId));
        }
        return bridge;
    }

    @Transactional
    public Bridge findByNameAndCustomerId(String bridgeName, String customerId) {
        return bridgeDAO.findByNameAndCustomerId(bridgeName, customerId);
    }

    @Override
    public void deleteBridge(String idOrName, String customerId) {
        Bridge bridge = doDeleteBridge(idOrName, customerId);

        workManager.schedule(bridge);
    }

    @Transactional
    protected Bridge doDeleteBridge(String idOrName, String customerId) {
        Bridge bridge = getBridgeByIdOrName(idOrName, customerId);
        Long processorsCount = processorService.getProcessorsCount(bridge);
        if (processorsCount > 0) {
            // See https://issues.redhat.com/browse/MGDOBR-43
            throw new BridgeLifecycleException("It is not possible to delete a Bridge instance with active Processors.");
        }
        bridge.setStatus(ManagedResourceStatus.DEPROVISION);
        LOGGER.info("Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());

        return bridge;
    }

    @Transactional
    @Override
    public ListResult<Bridge> getBridges(String customerId, QueryInfo queryInfo) {
        return bridgeDAO.findByCustomerId(customerId, queryInfo);
    }

    @Transactional
    @Override
    public List<Bridge> findByShardIdWithReadyDependencies(String shardId) {
        return bridgeDAO.findByShardIdWithReadyDependencies(shardId);
    }

    @Transactional
    @Override
    public Bridge updateBridge(BridgeDTO bridgeDTO) {
        Bridge bridge = findByIdAndCustomerId(bridgeDTO.getId(), bridgeDTO.getCustomerId());
        bridge.setStatus(bridgeDTO.getStatus());
        bridge.setEndpoint(bridgeDTO.getEndpoint());
        bridge.setModifiedAt(ZonedDateTime.now());

        if (bridgeDTO.getStatus().equals(ManagedResourceStatus.DELETED)) {
            bridgeDAO.deleteById(bridge.getId());
        }
        if (bridgeDTO.getStatus().equals(ManagedResourceStatus.READY) && Objects.isNull(bridge.getPublishedAt())) {
            bridge.setPublishedAt(ZonedDateTime.now());
        }

        // Update metrics
        meterRegistry.counter("manager.bridge.status.change",
                Collections.singletonList(Tag.of("status", bridgeDTO.getStatus().toString()))).increment();

        LOGGER.info("Bridge with id '{}' has been updated for customer '{}'", bridge.getId(), bridge.getCustomerId());
        return bridge;
    }

    @Override
    public BridgeDTO toDTO(Bridge bridge) {
        KafkaConnectionDTO kafkaConnectionDTO = new KafkaConnectionDTO(
                internalKafkaConfigurationProvider.getBootstrapServers(),
                internalKafkaConfigurationProvider.getClientId(),
                internalKafkaConfigurationProvider.getClientSecret(),
                internalKafkaConfigurationProvider.getSecurityProtocol(),
                resourceNamesProvider.getBridgeTopicName(bridge.getId()));
        BridgeDTO dto = new BridgeDTO();
        dto.setId(bridge.getId());
        dto.setName(bridge.getName());
        dto.setEndpoint(bridge.getEndpoint());
        dto.setStatus(bridge.getStatus());
        dto.setCustomerId(bridge.getCustomerId());
        dto.setKafkaConnection(kafkaConnectionDTO);
        return dto;
    }

    @Override
    public BridgeResponse toResponse(Bridge bridge) {
        BridgeResponse response = new BridgeResponse();
        response.setId(bridge.getId());
        response.setName(bridge.getName());
        response.setEndpoint(bridge.getEndpoint());
        response.setSubmittedAt(bridge.getSubmittedAt());
        response.setPublishedAt(bridge.getPublishedAt());
        response.setStatus(bridge.getStatus());
        response.setHref(APIConstants.USER_API_BASE_PATH + bridge.getId());
        return response;
    }

}
