package com.redhat.service.smartevents.manager.v2.services;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.AMSFailException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.TermsNotAcceptedYetException;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.core.dns.DnsService;
import com.redhat.service.smartevents.manager.core.services.ShardService;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import dev.bf2.ffm.ams.core.AccountManagementService;
import dev.bf2.ffm.ams.core.exceptions.TermsRequiredException;
import dev.bf2.ffm.ams.core.models.AccountInfo;
import dev.bf2.ffm.ams.core.models.CreateResourceRequest;
import dev.bf2.ffm.ams.core.models.ResourceCreated;
import dev.bf2.ffm.ams.core.models.TermsRequest;

import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getModifiedAt;

@ApplicationScoped
public class BridgeServiceImpl implements BridgeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeServiceImpl.class);

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

    @V2
    @Inject
    AccountManagementService accountManagementService;

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
        if (StatusUtilities.getManagedResourceStatus(bridge) != ManagedResourceStatus.READY) {
            throw new BridgeLifecycleException(String.format("Bridge with id '%s' for customer '%s' is not in the '%s' state.", bridge.getId(), bridge.getCustomerId(), ManagedResourceStatus.READY));
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

        // TODO: record metrics with MetricsService

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

        // TODO: record metrics with MetricsService.

        LOGGER.info("Bridge with id '{}' for customer '{}' has been marked for deletion", bridge.getId(), bridge.getCustomerId());
    }

    @Override
    public BridgeResponse toResponse(Bridge bridge) {
        BridgeResponse response = new BridgeResponse();
        response.setId(bridge.getId());
        response.setName(bridge.getName());
        response.setStatus(StatusUtilities.getManagedResourceStatus(bridge));
        // Return the endpoint only if the resource is READY or FAILED https://github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox/pull/1006#discussion_r937488097
        if (ManagedResourceStatus.READY.equals(response.getStatus()) || ManagedResourceStatus.FAILED.equals(response.getStatus())) {
            response.setEndpoint(bridge.getEndpoint());
        }
        response.setSubmittedAt(bridge.getSubmittedAt());
        response.setPublishedAt(bridge.getPublishedAt());
        response.setModifiedAt(getModifiedAt(bridge));
        response.setHref(V2APIConstants.V2_USER_API_BASE_PATH + bridge.getId());
        response.setOwner(bridge.getOwner());
        response.setCloudProvider(bridge.getCloudProvider());
        response.setRegion(bridge.getRegion());
        // TODO: add support for errors in v2 https://issues.redhat.com/browse/MGDOBR-1284
        // response.setStatusMessage(bridgeErrorHelper.makeUserMessage(bridge));

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
