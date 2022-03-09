package com.redhat.service.bridge.manager;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

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
    RhoasService rhoasService;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Inject
    ShardService shardService;

    @Transactional
    @Override
    public Bridge createBridge(String customerId, BridgeRequest bridgeRequest) {
        if (bridgeDAO.findByNameAndCustomerId(bridgeRequest.getName(), customerId).isPresent()) {
            throw new AlreadyExistingItemException(String.format("Bridge with name '%s' already exists for customer with id '%s'", bridgeRequest.getName(), customerId));
        }

        Bridge bridge = bridgeRequest.toEntity();
        bridge.setStatus(BridgeStatus.ACCEPTED);
        bridge.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        bridge.setCustomerId(customerId);
        bridge.setShardId(shardService.getAssignedShardId(bridge.getId()));
        bridgeDAO.persist(bridge);

        rhoasService.createTopicAndGrantAccessFor(getBridgeTopicName(bridge), RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        LOGGER.info("Bridge with id '{}' has been created for customer '{}'", bridge.getId(), bridge.getCustomerId());
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
    public boolean isBridgeReady(Bridge bridge) {
        return BridgeStatus.READY == bridge.getStatus();
    }

    @Override
    public Bridge getBridgeByBridgeIdentifier(String bridgeIdentifier, String customerId) {
        Optional<Bridge> bridgeOptional = getBridgeByIdAndCustomerId(bridgeIdentifier, customerId);
        if (bridgeOptional.isPresent()) {
            return bridgeOptional.get();
        }

        bridgeOptional = getBridgeByNameAndCustomerId(bridgeIdentifier, customerId);
        if (bridgeOptional.isPresent()) {
            return bridgeOptional.get();
        }
        throw new ItemNotFoundException(String.format("Bridge with identifier '%s' for customer '%s' does not exist", bridgeIdentifier, customerId));
    }

    @Transactional
    @Override
    public Optional<Bridge> getBridgeByIdAndCustomerId(String bridgeId, String customerId) {
        return bridgeDAO.findByIdAndCustomerId(bridgeId, customerId);
    }

    @Transactional
    public Optional<Bridge> getBridgeByNameAndCustomerId(String bridgeName, String customerId) {
        return bridgeDAO.findByNameAndCustomerId(bridgeName, customerId);
    }

    @Transactional
    @Override
    public void deleteBridge(String bridgeIdentifier, String customerId) {
        Bridge bridge = getBridgeByBridgeIdentifier(bridgeIdentifier, customerId);
        Long processorsCount = processorService.getProcessorsCount(bridge);
        if (processorsCount > 0) {
            // See https://issues.redhat.com/browse/MGDOBR-43
            throw new BridgeLifecycleException("It is not possible to delete a Bridge instance with active Processors.");
        }
        bridge.setStatus(BridgeStatus.DEPROVISION);
        LOGGER.info("Bridge with identifier '{}' for customer '{}' has been marked for deletion", bridgeIdentifier, customerId);
    }

    @Transactional
    @Override
    public ListResult<Bridge> getBridges(String customerId, QueryInfo queryInfo) {
        return bridgeDAO.findByCustomerId(customerId, queryInfo);
    }

    @Transactional
    @Override
    public List<Bridge> getBridgesByStatusesAndShardId(List<BridgeStatus> statuses, String shardId) {
        return bridgeDAO.findByStatusesAndShardId(statuses, shardId);
    }

    @Transactional
    @Override
    public Bridge updateBridge(BridgeDTO bridgeDTO) {
        Optional<Bridge> bridgeOptional = getBridgeByIdAndCustomerId(bridgeDTO.getId(), bridgeDTO.getCustomerId());
        if (bridgeOptional.isEmpty()) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' for customer '%s' does not exist", bridgeDTO.getId(), bridgeDTO.getCustomerId()));
        }
        Bridge bridge = bridgeOptional.get();
        bridge.setStatus(bridgeDTO.getStatus());
        bridge.setEndpoint(bridgeDTO.getEndpoint());

        if (bridgeDTO.getStatus().equals(BridgeStatus.DELETED)) {
            bridgeDAO.deleteById(bridge.getId());
            rhoasService.deleteTopicAndRevokeAccessFor(getBridgeTopicName(bridge), RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
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
                getBridgeTopicName(bridge));
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

    @Override
    public String getBridgeTopicName(Bridge bridge) {
        return internalKafkaConfigurationProvider.getTopicPrefix() + bridge.getId();
    }
}
