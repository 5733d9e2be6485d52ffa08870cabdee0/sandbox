package com.redhat.service.smartevents.manager;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.AMSFailException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.TermsNotAcceptedYetException;
import com.redhat.service.smartevents.infra.metrics.MetricsOperation;
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
import com.redhat.service.smartevents.manager.metrics.ManagedResourceOperationMapper.ManagedResourceOperation;
import com.redhat.service.smartevents.manager.metrics.ManagerMetricsService;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.processingerrors.ProcessingErrorService;

import dev.bf2.ffm.ams.core.AccountManagementService;
import dev.bf2.ffm.ams.core.exceptions.TermsRequiredException;
import dev.bf2.ffm.ams.core.models.AccountInfo;
import dev.bf2.ffm.ams.core.models.CreateResourceRequest;
import dev.bf2.ffm.ams.core.models.ResourceCreated;
import dev.bf2.ffm.ams.core.models.TermsRequest;

import static com.redhat.service.smartevents.manager.metrics.ManagedResourceOperationMapper.inferOperation;

@ApplicationScoped
public class BridgesServiceImpl implements BridgesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgesServiceImpl.class);

    private String tlsCertificate;

    private String tlsKey;

    // The tls certificate and the key are b64 encoded. See https://issues.redhat.com/browse/MGDOBR-1068 .
    @ConfigProperty(name = "event-bridge.dns.subdomain.tls.certificate")
    String b64TlsCertificate;

    @ConfigProperty(name = "event-bridge.dns.subdomain.tls.key")
    String b64TlsKey;

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
    ManagerMetricsService metricsService;

    @Inject
    DnsService dnsService;

    @Inject
    ProcessingErrorService processingErrorService;

    @Inject
    BridgeErrorHelper bridgeErrorHelper;

    @Inject
    AccountManagementService accountManagementService;

    @PostConstruct
    void init() {
        if (!Objects.isNull(b64TlsCertificate) && !b64TlsCertificate.isEmpty()) {
            LOGGER.info("Decoding base64 tls certificate and key");
            tlsCertificate = new String(Base64.getDecoder().decode(b64TlsCertificate));
            tlsKey = new String(Base64.getDecoder().decode(b64TlsKey));
        } else {
            LOGGER.info("Tls certificate and key were not configured.");
        }
    }

    @Override
    @Transactional
    public Bridge createBridge(String customerId, String organisationId, String owner, BridgeRequest bridgeRequest) {
        if (bridgeDAO.findByNameAndCustomerId(bridgeRequest.getName(), customerId) != null) {
            throw new AlreadyExistingItemException(String.format("Bridge with name '%s' already exists for customer with id '%s'", bridgeRequest.getName(), customerId));
        }

        // Create resource on AMS - raise an exception is the organisation is out of quota
        String subscriptionId = createResourceOnAMS(organisationId);

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
        bridge.setSubscriptionId(subscriptionId);

        //Ensure we connect the ErrorHandler Action to the ErrorHandler back-channel
        Action errorHandler = bridgeRequest.getErrorHandler();
        Action resolvedErrorHandler = processingErrorService.resolveAndUpdateErrorHandler(bridge.getId(), errorHandler);
        bridge.setDefinition(new BridgeDefinition(errorHandler, resolvedErrorHandler));

        // Bridge and Work creation should always be in the same transaction
        bridgeDAO.persist(bridge);
        workManager.schedule(bridge);
        metricsService.onOperationStart(bridge, MetricsOperation.MANAGER_RESOURCE_PROVISION);

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
        Action resolvedErrorHandler = processingErrorService.resolveAndUpdateErrorHandler(bridgeId, updatedErrorHandler);
        BridgeDefinition updatedDefinition = new BridgeDefinition(updatedErrorHandler, resolvedErrorHandler);

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
        existingBridge.setErrorId(null);
        existingBridge.setErrorUUID(null);

        // Bridge and Work should always be created in the same transaction
        workManager.schedule(existingBridge);
        metricsService.onOperationStart(existingBridge, MetricsOperation.MANAGER_RESOURCE_MODIFY);

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
        metricsService.onOperationStart(bridge, MetricsOperation.MANAGER_RESOURCE_DELETE);

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
    public Bridge updateBridgeStatus(ManagedResourceStatusUpdateDTO updateDTO) {
        Bridge bridge = getBridge(updateDTO.getId(), updateDTO.getCustomerId());
        ManagedResourceOperation operation = inferOperation(bridge, updateDTO);
        bridge.setStatus(updateDTO.getStatus());

        // If the User has updated a Bridge that was previously failed by k8s it has been observed
        // that the reconciliation loop can first emit an update with the existing FAILED state
        // to subsequently emit an update with a READY state when the CRD updates and succeeds.
        bridge.setErrorId(null);
        bridge.setErrorUUID(null);

        switch (operation) {
            case UNDETERMINED:
                break;
            case CREATE:
                bridge.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
                metricsService.onOperationComplete(bridge, MetricsOperation.MANAGER_RESOURCE_PROVISION);
                break;

            case UPDATE:
                metricsService.onOperationComplete(bridge, MetricsOperation.MANAGER_RESOURCE_MODIFY);
                break;

            case DELETE:
                accountManagementService.deleteResource(bridge.getSubscriptionId());
                bridgeDAO.deleteById(bridge.getId());
                metricsService.onOperationComplete(bridge, MetricsOperation.MANAGER_RESOURCE_DELETE);
                break;

            case FAILED_CREATE:
            case FAILED_UPDATE:
            case FAILED_DELETE:
                // If an exception happened; make sure to record it.
                BridgeErrorInstance bridgeErrorInstance = updateDTO.getBridgeErrorInstance();
                if (Objects.nonNull(bridgeErrorInstance)) {
                    bridge.setErrorId(bridgeErrorInstance.getId());
                    bridge.setErrorUUID(bridgeErrorInstance.getUuid());
                }

                metricsService.onOperationFailed(bridge, operation.getMetricsOperation());
                break;
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
        response.setStatusMessage(bridgeErrorHelper.makeUserMessage(bridge));

        return response;
    }

    private String createResourceOnAMS(String organisationId) {
        AccountInfo accountInfo = new AccountInfo.Builder()
                .withOrganizationId(organisationId)
                // TODO: properly populate these when we switch to AMS - https://issues.redhat.com/browse/MGDOBR-1166.
                .withAccountUsername("TODO")
                .withAccountId(0L)
                .withAdminRole(Boolean.FALSE)
                .build();

        CreateResourceRequest createResourceRequest = new CreateResourceRequest.Builder()
                .withCount(1)
                .withAccountInfo(accountInfo)
                // TODO: properly populate these when we switch to AMS - https://issues.redhat.com/browse/MGDOBR-1166.
                .withProductId("TODO")
                .withCloudProviderId("TODO")
                .withAvailabilityZoneType("TODO")
                .withResourceName("TODO")
                .withBillingModel("TODO")
                .withClusterId("TODO")
                .withTermRequest(new TermsRequest.Builder().withEventCode("TODO").withSiteCode("TODO").build())
                .build();

        try {
            ResourceCreated resourceCreated = accountManagementService.createResource(createResourceRequest).await().atMost(Duration.ofSeconds(5));
            return resourceCreated.getSubscriptionId();
        } catch (TermsRequiredException e) {
            throw new TermsNotAcceptedYetException("Terms must be accepted in order to use the service.");
        } catch (NoQuotaAvailable e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("An error occurred with AMS for the organisation '{}'", organisationId, e);
            throw new AMSFailException("Could not check if organization has quota to create the resource.");
        }
    }
}
