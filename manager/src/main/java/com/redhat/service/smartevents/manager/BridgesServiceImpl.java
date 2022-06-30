package com.redhat.service.smartevents.manager;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.bridges.BridgeDefinition;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.metrics.MetricsOperation;
import com.redhat.service.smartevents.manager.metrics.MetricsService;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.OrganisationServiceLimit;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.workers.WorkManager;

@ApplicationScoped
public class BridgesServiceImpl implements BridgesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgesServiceImpl.class);

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorService processorService;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    ShardService shardService;

    @Inject
    WorkManager workManager;

    @Inject
    MetricsService metricsService;

    @Inject
    LimitService limitService;

    @Override
    @Transactional
    public Bridge createBridge(String customerId, String organisationId, String owner, BridgeRequest bridgeRequest) {
        if (bridgeDAO.findByNameAndCustomerId(bridgeRequest.getName(), customerId) != null) {
            throw new AlreadyExistingItemException(String.format("Bridge with name '%s' already exists for customer with id '%s'", bridgeRequest.getName(), customerId));
        }

        Bridge bridge = bridgeRequest.toEntity();
        bridge.setStatus(ManagedResourceStatus.ACCEPTED);
        bridge.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        bridge.setCustomerId(customerId);
        bridge.setOrganisationId(organisationId);
        bridge.setOwner(owner);
        bridge.setShardId(shardService.getAssignedShardId(bridge.getId()));
        bridge.setGeneration(0);

        OrganisationServiceLimit organisationServiceLimit = limitService.getOrganisationServiceLimit(organisationId);
        bridge.setInstanceType(organisationServiceLimit.getInstanceType());
        String bridgeDuration = organisationServiceLimit.getBridgeDuration();
        if (StringUtils.isNotEmpty(bridgeDuration)) {
            Duration duration = Duration.parse(bridgeDuration);
            bridge.setExpireAt(ZonedDateTime.now().plus(duration));
        }

        //Ensure we connect the ErrorHandler Action to the ErrorHandler back-channel
        Action errorHandler = bridgeRequest.getErrorHandler();
        bridge.setDefinition(new BridgeDefinition(Objects.nonNull(errorHandler) ? errorHandler : null));

        // Bridge and Work creation should always be in the same transaction
        bridgeDAO.persist(bridge);
        workManager.schedule(bridge);
        metricsService.onOperationStart(bridge, MetricsOperation.PROVISION);

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
    public Bridge getReadyBridge(String bridgeId, String customerId) {
        Bridge bridge = getBridge(bridgeId, customerId);
        if (ManagedResourceStatus.READY != bridge.getStatus()) {
            throw new BridgeLifecycleException(String.format("Bridge with id '%s' for customer '%s' is not in the '%s' state.", bridge.getId(), bridge.getCustomerId(), ManagedResourceStatus.READY));
        }
        return bridge;
    }

    private Bridge findByIdAndCustomerId(String id, String customerId) {
        Bridge bridge = bridgeDAO.findByIdAndCustomerId(id, customerId);
        if (bridge == null) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' for customer '%s' does not exist", id, customerId));
        }
        return bridge;
    }

    @Transactional
    @Override
    public Bridge getBridge(String id, String customerId) {
        return findByIdAndCustomerId(id, customerId);
    }

    @Override
    @Transactional
    public void deleteBridge(String id, String customerId) {
        Long processorsCount = processorService.getProcessorsCount(id, customerId);
        ListResult<Processor> hiddenProcessors = processorService.getHiddenProcessors(id, customerId);

        if (processorsCount != hiddenProcessors.getTotal()) {
            // See https://issues.redhat.com/browse/MGDOBR-43
            throw new BridgeLifecycleException("It is not possible to delete a Bridge instance with active Processors.");
        }

        Bridge bridge = findByIdAndCustomerId(id, customerId);
        if (!isBridgeDeletable(bridge)) {
            throw new BridgeLifecycleException("Bridge could only be deleted if its in READY/FAILED state.");
        }

        hiddenProcessors.getItems().forEach(p -> processorService.deleteProcessor(id, p.getId(), customerId));

        LOGGER.info("Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());

        // Bridge deletion and related Work creation should always be in the same transaction
        bridge.setStatus(ManagedResourceStatus.DEPROVISION);
        bridge.setDeletionRequestedAt(ZonedDateTime.now());
        workManager.schedule(bridge);
        metricsService.onOperationStart(bridge, MetricsOperation.DELETE);

        LOGGER.info("Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());
    }

    private boolean isBridgeDeletable(Bridge bridge) {
        // bridge could only be deleted if its in READY or FAILED state
        return bridge.getStatus() == ManagedResourceStatus.READY || bridge.getStatus() == ManagedResourceStatus.FAILED;
    }

    @Transactional
    @Override
    public ListResult<Bridge> getBridges(String customerId, QueryResourceInfo queryInfo) {
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
        Bridge bridge = getBridge(bridgeDTO.getId(), bridgeDTO.getCustomerId());
        bridge.setStatus(bridgeDTO.getStatus());
        bridge.setEndpoint(bridgeDTO.getEndpoint());
        bridge.setModifiedAt(ZonedDateTime.now());

        if (bridgeDTO.getStatus().equals(ManagedResourceStatus.DELETED)) {
            bridgeDAO.deleteById(bridge.getId());
            metricsService.onOperationComplete(bridge, MetricsOperation.DELETE);
        } else if (bridgeDTO.getStatus().equals(ManagedResourceStatus.READY)) {
            if (Objects.isNull(bridge.getPublishedAt())) {
                bridge.setPublishedAt(ZonedDateTime.now());
                metricsService.onOperationComplete(bridge, MetricsOperation.PROVISION);
            }
        }

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
                internalKafkaConfigurationProvider.getSaslMechanism(),
                resourceNamesProvider.getBridgeTopicName(bridge.getId()),
                resourceNamesProvider.getBridgeErrorTopicName(bridge.getId()));
        BridgeDTO dto = new BridgeDTO();
        dto.setId(bridge.getId());
        dto.setName(bridge.getName());
        dto.setEndpoint(bridge.getEndpoint());
        dto.setStatus(bridge.getStatus());
        dto.setCustomerId(bridge.getCustomerId());
        dto.setOwner(bridge.getOwner());
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
        response.setOwner(bridge.getOwner());
        response.setErrorHandler(bridge.getDefinition().getErrorHandler());
        return response;
    }

    @Override
    public long getActiveBridgeCount(String orgId) {
        return bridgeDAO.findByOrganisationId(orgId).stream().filter(this::isBridgeActive).count();
    }

    @Override
    public boolean isBridgeActive(String id) {
        Bridge bridge = getBridge(id);
        return isBridgeActive(bridge);
    }

    private boolean isBridgeActive(Bridge bridge) {
        ZonedDateTime expireAt = bridge.getExpireAt();
        if (Objects.isNull(expireAt)) {
            return true;
        }

        return ZonedDateTime.now().isBefore(expireAt);
    }
}
