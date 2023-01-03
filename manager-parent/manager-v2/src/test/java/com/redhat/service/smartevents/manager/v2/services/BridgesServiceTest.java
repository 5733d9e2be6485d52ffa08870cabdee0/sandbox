package com.redhat.service.smartevents.manager.v2.services;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.DEPROVISION;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ENDPOINT;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_TLS_CERTIFICATE;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_TLS_KEY;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CLOUD_PROVIDER;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_ORGANISATION_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_REGION;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createDeprovisioningBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProvisioningBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createReadyBridge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class BridgesServiceTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    BridgeService bridgeService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    public void testCreateBridge() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        Bridge createdBridge = bridgeService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        Bridge retrieved = bridgeDAO.findByIdWithConditions(createdBridge.getId());

        assertThat(retrieved.getId()).isEqualTo(createdBridge.getId());
        assertThat(retrieved.getName()).isEqualTo(createdBridge.getName());
        assertThat(retrieved.getEndpoint()).isEqualTo(createdBridge.getEndpoint());
        assertThat(retrieved.getSubmittedAt()).isEqualTo(createdBridge.getSubmittedAt());
        assertThat(retrieved.getPublishedAt()).isEqualTo(createdBridge.getPublishedAt());
        assertThat(retrieved.getOperation().getRequestedAt()).isEqualTo(createdBridge.getOperation().getRequestedAt());
        assertThat(retrieved.getConditions()).hasSize(3);
        assertThat(retrieved.getConditions().stream().allMatch(x -> x.getStatus().equals(ConditionStatus.UNKNOWN))).isTrue();
        assertThat(retrieved.getConditions().stream().anyMatch(x -> x.getType().equals(DefaultConditions.CP_DATA_PLANE_READY_NAME))).isTrue();
        assertThat(retrieved.getConditions().stream().anyMatch(x -> x.getType().equals(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME))).isTrue();
        assertThat(retrieved.getConditions().stream().anyMatch(x -> x.getType().equals(DefaultConditions.CP_DNS_RECORD_READY_NAME))).isTrue();
        assertThat(retrieved.getOwner()).isEqualTo(createdBridge.getOwner());
        assertThat(retrieved.getCloudProvider()).isEqualTo(createdBridge.getCloudProvider());
        assertThat(retrieved.getRegion()).isEqualTo(createdBridge.getRegion());
    }

    @Test
    void testCreateBridge_whiteSpaceInName() {
        BridgeRequest request = new BridgeRequest("   name   ", DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        Bridge bridge = bridgeService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);
        assertThat(bridge.getName()).isEqualTo("name");
    }

    @Test
    void testToResponse() {
        Bridge bridge = createBridgeWithAcceptedConditions();

        BridgeResponse response = bridgeService.toResponse(bridge);

        assertThat(response.getId()).isEqualTo(bridge.getId());
        assertThat(response.getName()).isEqualTo(bridge.getName());
        assertThat(response.getEndpoint()).isNull();
        assertThat(response.getSubmittedAt()).isEqualTo(bridge.getSubmittedAt());
        assertThat(response.getPublishedAt()).isEqualTo(bridge.getPublishedAt());
        assertThat(response.getModifiedAt()).isNull();
        assertThat(response.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
        assertThat(response.getHref()).contains(bridge.getId());
        assertThat(response.getOwner()).isEqualTo(bridge.getOwner());
        assertThat(response.getCloudProvider()).isEqualTo(bridge.getCloudProvider());
        assertThat(response.getRegion()).isEqualTo(bridge.getRegion());
        assertThat(response.getStatusMessage()).isNull();
    }

    @Test
    void testOrganisationWithNoQuota() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        assertThatExceptionOfType(NoQuotaAvailable.class).isThrownBy(() -> bridgeService.createBridge(DEFAULT_CUSTOMER_ID, "organisation_with_no_quota", DEFAULT_USER_NAME, request));
    }

    protected Bridge createBridgeWithAcceptedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.MANAGER, ZonedDateTime.now(ZoneOffset.UTC)));
        conditions.add(new Condition(DefaultConditions.CP_DNS_RECORD_READY_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.MANAGER, ZonedDateTime.now(ZoneOffset.UTC)));
        conditions.add(new Condition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.SHARD, ZonedDateTime.now(ZoneOffset.UTC)));

        Bridge b = Fixtures.createBridge();
        b.setConditions(conditions);
        bridgeDAO.persist(b);

        return b;
    }

    @Test
    public void testGetBridge_Found() {
        Bridge bridge = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        assertThat(bridgeService.getBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).isEqualTo(bridge);
    }

    @Test
    public void testGetBridge_NotFound() {
        assertThatThrownBy(() -> bridgeService.getBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    public void testGetReadyBridge_FoundReady() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        assertThat(bridgeService.getReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID)).isEqualTo(bridge);
    }

    @Test
    public void testGetReadyBridge_FoundNotReady() {
        Bridge bridge = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> bridgeService.getReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID));
    }

    @Test
    public void testGetReadyBridge_NotFound() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgeService.getReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID));
    }

    @Test
    public void testDeleteBridge() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        bridgeService.deleteBridge(bridge.getId(), bridge.getCustomerId());

        Bridge retrievedBridge = bridgeService.getBridge(bridge.getId(), bridge.getCustomerId());
        assertThat(StatusUtilities.getManagedResourceStatus(retrievedBridge)).isEqualTo(DEPROVISION);
        assertThat(retrievedBridge.getOperation().getType()).isEqualTo(OperationType.DELETE);
    }

    @Test
    public void testDeleteBridge_whenStatusIsFailed() {
        Bridge bridge = Fixtures.createFailedBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        bridgeService.deleteBridge(bridge.getId(), bridge.getCustomerId());

        Bridge retrievedBridge = bridgeService.getBridge(bridge.getId(), bridge.getCustomerId());
        assertThat(StatusUtilities.getManagedResourceStatus(retrievedBridge)).isEqualTo(DEPROVISION);
        assertThat(retrievedBridge.getOperation().getType()).isEqualTo(OperationType.DELETE);
    }

    @Test
    public void testDeleteBridge_whenStatusIsNotReady() {
        Bridge bridge = createProvisioningBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> bridgeService.deleteBridge(bridge.getId(), bridge.getCustomerId()));
    }

    @Test
    void testFindByShardIdToDeployOrDelete() {
        Bridge bridge = createProvisioningBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        List<Bridge> bridges = bridgeService.findByShardIdToDeployOrDelete(TestConstants.SHARD_ID);
        assertThat(bridges).hasSize(1);
        assertThat(bridges.get(0).getId()).isEqualTo(bridge.getId());
    }

    @Test
    public void testToDTO() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        BridgeDTO bridgeDTO = bridgeService.toDTO(bridge);

        assertThat(bridgeDTO.getId()).hasSizeGreaterThan(0);
        assertThat(bridgeDTO.getName()).isEqualTo(DEFAULT_BRIDGE_NAME);
        assertThat(bridgeDTO.getEndpoint()).isEqualTo(DEFAULT_BRIDGE_ENDPOINT);
        assertThat(bridgeDTO.getCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
        assertThat(bridgeDTO.getOwner()).isEqualTo(DEFAULT_CUSTOMER_ID);
        assertThat(bridgeDTO.getTlsCertificate()).isEqualTo(DEFAULT_BRIDGE_TLS_CERTIFICATE);
        assertThat(bridgeDTO.getTlsKey()).isEqualTo(DEFAULT_BRIDGE_TLS_KEY);
        assertThat(bridgeDTO.getGeneration()).isEqualTo(bridge.getGeneration());
        assertThat(bridgeDTO.getOperationType()).isEqualTo(bridge.getOperation().getType());
    }

    @Test
    void testUpdateBridgeStatus_Create() {
        Bridge bridge = createProvisioningBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        assertThat(bridge.getPublishedAt()).isNull();

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(bridge.getId());
        statusDTO.setGeneration(bridge.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC))));

        bridgeService.updateBridgeStatus(statusDTO);

        assertThat(bridgeDAO.findById(bridge.getId()).getPublishedAt()).isNotNull();
    }

    @Test
    void testUpdateBridgeStatus_CreateWithSuccessiveUpdate() {
        Bridge bridge = createProvisioningBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        assertThat(bridge.getPublishedAt()).isNull();

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(bridge.getId());
        statusDTO.setGeneration(bridge.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC))));

        Bridge updated = bridgeService.updateBridgeStatus(statusDTO);

        assertThat(updated.getPublishedAt()).isNotNull();

        Bridge updated2 = bridgeService.updateBridgeStatus(statusDTO);

        assertThat(updated2.getPublishedAt()).isEqualTo(updated.getPublishedAt());
    }

    @Test
    void testUpdateBridgeStatus_CreateWhenIncomplete() {
        Bridge bridge = createProvisioningBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        assertThat(bridge.getPublishedAt()).isNull();

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(bridge.getId());
        statusDTO.setGeneration(bridge.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.FALSE, ZonedDateTime.now(ZoneOffset.UTC))));

        bridgeService.updateBridgeStatus(statusDTO);

        assertThat(bridge.getPublishedAt()).isNull();
    }

    @Test
    @Disabled("Nothing to assert until Metrics are added back. See MGDOBR-1340.")
    void testUpdateBridgeStatus_Update() {
        Bridge bridge = createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        Operation operation = new Operation();
        operation.setType(OperationType.UPDATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        bridge.setOperation(operation);
        bridgeDAO.persist(bridge);

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(bridge.getId());
        statusDTO.setGeneration(bridge.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC))));

        bridgeService.updateBridgeStatus(statusDTO);

        assertThat(bridge.getPublishedAt()).isNotNull();
    }

    @Test
    void testUpdateBridgeStatus_Delete() {
        Bridge bridge = createDeprovisioningBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(bridge.getId());
        statusDTO.setGeneration(bridge.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.FALSE, ZonedDateTime.now(ZoneOffset.UTC))));

        bridgeService.updateBridgeStatus(statusDTO);
        assertThat(bridgeDAO.findById(bridge.getId())).isNotNull();
    }

    @Test
    void testUpdateBridgeStatus_DeleteWhenIncomplete() {
        Bridge bridge = createDeprovisioningBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        ResourceStatusDTO statusDTO = new ResourceStatusDTO();
        statusDTO.setId(bridge.getId());
        statusDTO.setGeneration(bridge.getGeneration());
        statusDTO.setConditions(List.of(new ConditionDTO(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC))));

        bridgeService.updateBridgeStatus(statusDTO);

        assertThat(bridgeDAO.findById(bridge.getId())).isNull();
    }
}
