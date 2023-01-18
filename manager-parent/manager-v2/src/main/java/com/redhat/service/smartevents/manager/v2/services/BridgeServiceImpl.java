package com.redhat.service.smartevents.manager.v2.services;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform.AMSFailException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.TermsNotAcceptedYetException;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.DNSConfigurationDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.KnativeBrokerConfigurationDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.infra.v2.api.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.core.dns.DnsService;
import com.redhat.service.smartevents.manager.core.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.core.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.core.services.ShardService;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.metrics.ManagerMetricsServiceV2;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.utils.ConditionUtilities;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import dev.bf2.ffm.ams.core.AccountManagementService;
import dev.bf2.ffm.ams.core.exceptions.TermsRequiredException;
import dev.bf2.ffm.ams.core.models.AccountInfo;
import dev.bf2.ffm.ams.core.models.CreateResourceRequest;
import dev.bf2.ffm.ams.core.models.ResourceCreated;
import dev.bf2.ffm.ams.core.models.TermsRequest;

import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getModifiedAt;
import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getStatusMessage;

@ApplicationScoped
public class BridgeServiceImpl implements BridgeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeServiceImpl.class);

    private String tlsCertificate;

    private String tlsKey;

    // The tls certificate and the key are b64 encoded. See https://issues.redhat.com/browse/MGDOBR-1068 .
    @ConfigProperty(name = "event-bridge.dns.subdomain.tls.certificate")
    String b64TlsCertificate;

    @ConfigProperty(name = "event-bridge.dns.subdomain.tls.key")
    String b64TlsKey;

    @ConfigProperty(name = "event-bridge.managed-bridge.deployment.timeout-seconds")
    int managedBridgeTimeoutSeconds;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ShardService shardService;

    @Inject
    ProcessorService processorService;

    @Inject
    DnsService dnsService;

    @V2
    @Inject
    WorkManager workManager;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @V2
    @Inject
    AccountManagementService accountManagementService;

    @Inject
    ManagerMetricsServiceV2 metricsService;

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
    public Bridge getBridge(String bridgeId, String customerId) {
        Bridge bridge = bridgeDAO.findByIdAndCustomerIdWithConditions(bridgeId, customerId);
        if (Objects.isNull(bridge)) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' for customer '%s' does not exist", bridgeId, customerId));
        }
        return bridge;
    }

    @Override
    @Transactional
    public Bridge getReadyBridge(String bridgeId, String customerId) {
        Bridge bridge = getBridge(bridgeId, customerId);
        if (StatusUtilities.getManagedResourceStatus(bridge) != ManagedResourceStatusV2.READY) {
            throw new BridgeLifecycleException(String.format("Bridge with id '%s' for customer '%s' is not in the '%s' state.", bridge.getId(), bridge.getCustomerId(), ManagedResourceStatusV2.READY));
        }
        return bridge;
    }

    @Override
    @Transactional
    public Bridge createBridge(String customerId, String organisationId, String owner, BridgeRequest bridgeRequest) {
        if (bridgeDAO.findByNameAndCustomerId(bridgeRequest.getName(), customerId) != null) {
            throw new AlreadyExistingItemException(String.format("Bridge with name '%s' already exists for customer with id '%s'", bridgeRequest.getName(), customerId));
        }

        Operation operation = new Operation();
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        operation.setType(OperationType.CREATE);

        // Create resource on AMS - raise an exception is the organisation is out of quota
        String subscriptionId = createResourceOnAMS(organisationId);

        Bridge bridge = bridgeRequest.toEntity();
        bridge.setConditions(createAcceptedConditions());
        bridge.setOperation(operation);
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

        // Bridge and Work creation should always be in the same transaction
        bridgeDAO.persist(bridge);
        workManager.schedule(bridge);

        metricsService.onOperationStart(bridge, MetricsOperation.MANAGER_RESOURCE_PROVISION);

        LOGGER.info("Bridge with id '{}' has been created for customer '{}'", bridge.getId(), bridge.getCustomerId());

        return bridge;
    }

    @Transactional
    @Override
    public ListResult<Bridge> getBridges(String customerId, QueryResourceInfo queryInfo) {
        return bridgeDAO.findByCustomerId(customerId, queryInfo);
    }

    @Override
    @Transactional
    public void deleteBridge(String id, String customerId) {
        Long processorsCount = processorService.getProcessorsCount(id, customerId);

        if (processorsCount > 0) {
            // See https://issues.redhat.com/browse/MGDOBR-43
            throw new BridgeLifecycleException("It is not possible to delete a Bridge instance with active Processors.");
        }

        Bridge bridge = bridgeDAO.findByIdAndCustomerIdWithConditions(id, customerId);
        if (bridge == null) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' for customer '%s' does not exist", id, customerId));
        }

        if (!StatusUtilities.isActionable(bridge)) {
            throw new BridgeLifecycleException("Bridge could only be deleted if its in READY/FAILED state.");
        }

        Operation operation = new Operation();
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        operation.setType(OperationType.DELETE);
        bridge.setOperation(operation);
        bridge.setConditions(createDeletedConditions());

        LOGGER.info("Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());

        // Bridge deletion and related Work creation should always be in the same transaction
        workManager.schedule(bridge);

        metricsService.onOperationStart(bridge, MetricsOperation.MANAGER_RESOURCE_DELETE);

        LOGGER.info("Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());
    }

    @Override
    public List<Bridge> findByShardIdToDeployOrDelete(String shardId) {
        return bridgeDAO.findByShardIdToDeployOrDelete(shardId);
    }

    @Override
    @Transactional
    public Bridge updateBridgeStatus(ResourceStatusDTO statusDTO) {
        Bridge bridge = bridgeDAO.findByIdWithConditions(statusDTO.getId());
        if (Objects.isNull(bridge)) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' does not exist.", statusDTO.getId()));
        }
        if (bridge.getGeneration() != statusDTO.getGeneration()) {
            LOGGER.info("Update for Bridge with id '{}' was discarded. The expected generation '{}' did not match the actual '{}'.",
                    bridge.getId(),
                    bridge.getGeneration(),
                    statusDTO.getGeneration());
            return bridge;
        }

        Operation operation = bridge.getOperation();
        List<Condition> conditions = bridge.getConditions();
        // Set the updated conditions to the existing Manager conditions to begin; then copy in the new Operator conditions
        List<Condition> updatedConditions = conditions.stream().filter(c -> c.getComponent() == ComponentType.MANAGER).collect(Collectors.toList());
        statusDTO.getConditions().forEach(c -> updatedConditions.add(Condition.from(c, ComponentType.SHARD)));
        bridge.setConditions(updatedConditions);

        // Don't do anything if the Operation is complete.
        if (Objects.nonNull(operation.getCompletedAt())) {
            LOGGER.info("Update for Bridge with id '{}' was discarded. The Operation has already been completed.", bridge.getId());
            return bridge;
        }

        switch (operation.getType()) {
            case CREATE:
                if (ConditionUtilities.isOperationComplete(updatedConditions)) {
                    operation.setCompletedAt(ZonedDateTime.now(ZoneOffset.UTC));
                    if (Objects.isNull(bridge.getPublishedAt())) {
                        bridge.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
                        metricsService.onOperationComplete(bridge, MetricsOperation.MANAGER_RESOURCE_PROVISION);
                    }
                } else if (ConditionUtilities.isOperationFailed(updatedConditions)) {
                    operation.setCompletedAt(ZonedDateTime.now(ZoneOffset.UTC));
                    metricsService.onOperationFailed(bridge, MetricsOperation.MANAGER_RESOURCE_PROVISION);
                }
                break;

            case UPDATE:
                if (ConditionUtilities.isOperationComplete(updatedConditions)) {
                    operation.setCompletedAt(ZonedDateTime.now(ZoneOffset.UTC));
                    metricsService.onOperationComplete(bridge, MetricsOperation.MANAGER_RESOURCE_UPDATE);
                } else if (ConditionUtilities.isOperationFailed(updatedConditions)) {
                    operation.setCompletedAt(ZonedDateTime.now(ZoneOffset.UTC));
                    metricsService.onOperationFailed(bridge, MetricsOperation.MANAGER_RESOURCE_UPDATE);
                }
                break;

            case DELETE:
                if (ConditionUtilities.isOperationComplete(updatedConditions)) {
                    // There is no need to check if the Bridge exists as any subsequent Status Update cycle
                    // would not include the same Bridge if it had been deleted. It would not have existed
                    // on the database and hence would not have been included in the Status Update cycle.
                    bridgeDAO.deleteById(statusDTO.getId());
                    metricsService.onOperationComplete(bridge, MetricsOperation.MANAGER_RESOURCE_DELETE);
                } else if (ConditionUtilities.isOperationFailed(updatedConditions)) {
                    operation.setCompletedAt(ZonedDateTime.now(ZoneOffset.UTC));
                    metricsService.onOperationFailed(bridge, MetricsOperation.MANAGER_RESOURCE_DELETE);
                }
                break;
        }

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
        KnativeBrokerConfigurationDTO knativeBrokerConfiguration = new KnativeBrokerConfigurationDTO(kafkaConnectionDTO);

        DNSConfigurationDTO dnsConfiguration = new DNSConfigurationDTO(
                bridge.getEndpoint(),
                tlsCertificate,
                tlsKey);

        BridgeDTO dto = new BridgeDTO();
        dto.setId(bridge.getId());
        dto.setName(bridge.getName());
        dto.setCustomerId(bridge.getCustomerId());
        dto.setOwner(bridge.getOwner());
        dto.setOperationType(bridge.getOperation().getType());
        dto.setGeneration(bridge.getGeneration());
        dto.setDnsConfiguration(dnsConfiguration);
        dto.setKnativeBrokerConfiguration(knativeBrokerConfiguration);
        dto.setTimeoutSeconds(managedBridgeTimeoutSeconds);
        return dto;
    }

    @Override
    public BridgeResponse toResponse(Bridge bridge) {
        BridgeResponse response = new BridgeResponse();
        response.setId(bridge.getId());
        response.setName(bridge.getName());
        response.setStatus(StatusUtilities.getManagedResourceStatus(bridge));
        // Return the endpoint only if the resource is READY or FAILED https://github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox/pull/1006#discussion_r937488097
        if (ManagedResourceStatusV2.READY.equals(response.getStatus()) || ManagedResourceStatusV2.FAILED.equals(response.getStatus())) {
            response.setEndpoint(bridge.getEndpoint());
        }
        response.setSubmittedAt(bridge.getSubmittedAt());
        response.setPublishedAt(bridge.getPublishedAt());
        response.setModifiedAt(getModifiedAt(bridge));
        response.setHref(V2APIConstants.V2_USER_API_BASE_PATH + bridge.getId());
        response.setOwner(bridge.getOwner());
        response.setCloudProvider(bridge.getCloudProvider());
        response.setRegion(bridge.getRegion());
        response.setStatusMessage(getStatusMessage(bridge));

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

    private List<Condition> createDeletedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(DefaultConditions.CP_KAFKA_TOPIC_DELETED_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.MANAGER, ZonedDateTime.now(ZoneOffset.UTC)));
        conditions.add(new Condition(DefaultConditions.CP_DNS_RECORD_DELETED_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.MANAGER, ZonedDateTime.now(ZoneOffset.UTC)));
        conditions.add(new Condition(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.SHARD, ZonedDateTime.now(ZoneOffset.UTC)));
        return conditions;
    }

    private List<Condition> createAcceptedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.MANAGER, ZonedDateTime.now(ZoneOffset.UTC)));
        conditions.add(new Condition(DefaultConditions.CP_DNS_RECORD_READY_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.MANAGER, ZonedDateTime.now(ZoneOffset.UTC)));
        conditions.add(new Condition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.SHARD, ZonedDateTime.now(ZoneOffset.UTC)));
        return conditions;
    }
}
