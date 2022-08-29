package com.redhat.service.smartevents.manager;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.bridges.BridgeDefinition;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.dns.DnsService;
import com.redhat.service.smartevents.manager.metrics.MetricsOperation;
import com.redhat.service.smartevents.manager.metrics.MetricsService;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.workers.WorkManager;

@ApplicationScoped
public class BridgesServiceImpl implements BridgesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgesServiceImpl.class);

    @ConfigProperty(name = "event-bridge.dns.subdomain.tls.certificate")
    String tlsCertificate;

    @ConfigProperty(name = "event-bridge.dns.subdomain.tls.key")
    String tlsKey;

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
    DnsService dnsService;

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
        bridge.setShardId(shardService.getAssignedShard(bridge.getId()).getId());
        bridge.setGeneration(0);
        bridge.setCloudProvider(bridgeRequest.getCloudProvider());
        bridge.setRegion(bridgeRequest.getRegion());
        bridge.setEndpoint(dnsService.buildBridgeEndpoint(bridge.getId(), customerId));

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

    @Override
    @Transactional
    public Bridge updateBridge(String bridgeId, String customerId, BridgeRequest bridgeRequest) {
        // Extract existing definition
        Bridge existingBridge = getBridge(bridgeId, customerId);

        if (!existingBridge.isActionable()) {
            throw new BridgeLifecycleException(String.format("Bridge with id '%s' for customer '%s' is not in an actionable state.",
                    bridgeId,
                    customerId));
        }

        BridgeDefinition existingDefinition = existingBridge.getDefinition();

        // Validate update.
        // Name cannot be updated.
        if (!Objects.equals(existingBridge.getName(), bridgeRequest.getName())) {
            throw new BadRequestException("It is not possible to update the Bridge's name.");
        }
        // Cloud Provider cannot be updated.
        if (!Objects.equals(existingBridge.getCloudProvider(), bridgeRequest.getCloudProvider())) {
            throw new BadRequestException("It is not possible to update the Bridge's Cloud Provider.");
        }
        // Cloud Region cannot be updated.
        if (!Objects.equals(existingBridge.getRegion(), bridgeRequest.getRegion())) {
            throw new BadRequestException("It is not possible to update the Bridge's Region.");
        }

        // Construct updated definition
        Action updatedErrorHandler = bridgeRequest.getErrorHandler();
        BridgeDefinition updatedDefinition = new BridgeDefinition(updatedErrorHandler);

        // No need to update CRD if the definition is unchanged
        if (Objects.equals(existingDefinition, updatedDefinition)) {
            return existingBridge;
        }

        // Create new definition copying existing properties
        existingBridge.setModifiedAt(ZonedDateTime.now());
        existingBridge.setStatus(ManagedResourceStatus.ACCEPTED);
        existingBridge.setDependencyStatus(ManagedResourceStatus.ACCEPTED);
        existingBridge.setDefinition(updatedDefinition);
        existingBridge.setGeneration(existingBridge.getGeneration() + 1);

        // Bridge and Work should always be created in the same transaction
        workManager.schedule(existingBridge);
        metricsService.onOperationStart(existingBridge, MetricsOperation.MODIFY);

        LOGGER.info("Bridge with id '{}' for customer '{}' has been marked for update",
                existingBridge.getId(),
                existingBridge.getCustomerId());

        return existingBridge;
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
        Optional<Processor> errorHandler = processorService.getErrorHandler(id, customerId);

        if (processorsCount > (errorHandler.isPresent() ? 1 : 0)) {
            // See https://issues.redhat.com/browse/MGDOBR-43
            throw new BridgeLifecycleException("It is not possible to delete a Bridge instance with active Processors.");
        }

        Bridge bridge = findByIdAndCustomerId(id, customerId);
        if (!bridge.isActionable()) {
            throw new BridgeLifecycleException("Bridge could only be deleted if its in READY/FAILED state.");
        }

        LOGGER.info("Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());

        // Bridge deletion and related Work creation should always be in the same transaction
        bridge.setStatus(ManagedResourceStatus.DEPROVISION);
        bridge.setDeletionRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        workManager.schedule(bridge);
        metricsService.onOperationStart(bridge, MetricsOperation.DELETE);

        LOGGER.info("Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());
    }

    @Transactional
    @Override
    public ListResult<Bridge> getBridges(String customerId, QueryResourceInfo queryInfo) {
        return bridgeDAO.findByCustomerId(customerId, queryInfo);
    }

    @Transactional
    @Override
    public List<Bridge> findByShardIdToDeployOrDelete(String shardId) {
        return bridgeDAO.findByShardIdToDeployOrDelete(shardId);
    }

    @Transactional
    @Override
    public Bridge updateBridge(ManagedResourceStatusUpdateDTO updateDTO) {
        Bridge bridge = getBridge(updateDTO.getId(), updateDTO.getCustomerId());
        bridge.setStatus(updateDTO.getStatus());

        if (updateDTO.getStatus().equals(ManagedResourceStatus.DELETED)) {
            bridgeDAO.deleteById(bridge.getId());
            metricsService.onOperationComplete(bridge, MetricsOperation.DELETE);
        } else if (updateDTO.getStatus().equals(ManagedResourceStatus.READY)) {
            if (Objects.isNull(bridge.getPublishedAt())) {
                bridge.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
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
        dto.setTlsCertificate(tlsCertificate);
        dto.setTlsKey(tlsKey);
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
        // Return the endpoint only if the resource is READY or FAILED https://github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox/pull/1006#discussion_r937488097
        if (ManagedResourceStatus.READY.equals(bridge.getStatus()) || ManagedResourceStatus.FAILED.equals(bridge.getStatus())) {
            response.setEndpoint(bridge.getEndpoint());
        }
        response.setSubmittedAt(bridge.getSubmittedAt());
        response.setPublishedAt(bridge.getPublishedAt());
        response.setModifiedAt(bridge.getModifiedAt());
        response.setStatus(bridge.getStatus());
        response.setHref(APIConstants.USER_API_BASE_PATH + bridge.getId());
        response.setOwner(bridge.getOwner());
        response.setErrorHandler(bridge.getDefinition().getErrorHandler());
        response.setCloudProvider(bridge.getCloudProvider());
        response.setRegion(bridge.getRegion());
        return response;
    }
}
