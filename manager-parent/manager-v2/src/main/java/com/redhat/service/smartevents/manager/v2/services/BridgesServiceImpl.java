package com.redhat.service.smartevents.manager.v2.services;

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

import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.core.api.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.AMSFailException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.TermsNotAcceptedYetException;
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.infra.v1.api.V1;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.infra.v1.api.models.bridges.BridgeDefinition;
import com.redhat.service.smartevents.infra.v1.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.manager.core.dns.DnsService;
import com.redhat.service.smartevents.manager.core.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.core.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.core.services.ShardService;
import com.redhat.service.smartevents.manager.v1.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v1.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v1.metrics.ManagedResourceOperationMapper.ManagedResourceOperation;
import com.redhat.service.smartevents.manager.v1.metrics.ManagerMetricsServiceV1;
import com.redhat.service.smartevents.manager.v1.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v1.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v1.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;
import dev.bf2.ffm.ams.core.AccountManagementService;
import dev.bf2.ffm.ams.core.exceptions.TermsRequiredException;
import dev.bf2.ffm.ams.core.models.AccountInfo;
import dev.bf2.ffm.ams.core.models.CreateResourceRequest;
import dev.bf2.ffm.ams.core.models.ResourceCreated;
import dev.bf2.ffm.ams.core.models.TermsRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.redhat.service.smartevents.manager.v1.metrics.ManagedResourceOperationMapper.inferOperation;

@ApplicationScoped
public class BridgesServiceImpl implements BridgesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgesServiceImpl.class);

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ShardService shardService;

    @Inject
    DnsService dnsService;

    @V2
    @Inject
    AccountManagementService accountManagementService;

    @Override
    @Transactional
    public Bridge createBridge(String customerId, String organisationId, String owner, BridgeRequest bridgeRequest) {
        if (bridgeDAO.findByNameAndCustomerId(bridgeRequest.getName(), customerId) != null) {
            throw new AlreadyExistingItemException(String.format("Bridge with name '%s' already exists for customer with id '%s'", bridgeRequest.getName(), customerId));
        }

        // Create resource on AMS - raise an exception is the organisation is out of quota
        String subscriptionId = createResourceOnAMS(organisationId);

        Bridge bridge = bridgeRequest.toEntity();
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

        // TODO: schedule work for dependencies

        // TODO: record metrics with MetricsService

        LOGGER.info("Bridge with id '{}' has been created for customer '{}'", bridge.getId(), bridge.getCustomerId());

        return bridge;
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
        response.setModifiedAt(bridge.getOperation().getRequestedAt());
        response.setHref(V2APIConstants.V2_USER_API_BASE_PATH + bridge.getId());
        response.setOwner(bridge.getOwner());
        response.setCloudProvider(bridge.getCloudProvider());
        response.setRegion(bridge.getRegion());
//        response.setStatusMessage(bridgeErrorHelper.makeUserMessage(bridge));

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
