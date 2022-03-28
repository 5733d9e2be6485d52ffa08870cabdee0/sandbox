package com.redhat.service.bridge.manager;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.Fixtures;
import com.redhat.service.bridge.manager.utils.TestUtils;
import com.redhat.service.bridge.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
@TestProfile(WorkerSchedulerProfile.class)
@QuarkusTestResource(PostgresResource.class)
public class BridgesServiceTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    BridgesService bridgesService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    RhoasService rhoasServiceMock;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    public void testGetEmptyBridgesToDeploy() {
        List<Bridge> bridges = bridgesService.findByShardIdWithReadyDependencies(TestConstants.SHARD_ID);
        assertThat(bridges.size()).isZero();
    }

    @Test
    public void testGetEmptyBridges() {
        ListResult<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE));
        assertThat(bridges.getPage()).isZero();
        assertThat(bridges.getTotal()).isZero();
        assertThat(bridges.getSize()).isZero();
    }

    @Test
    public void testGetBridges() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        ListResult<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isEqualTo(1);
        assertThat(bridges.getTotal()).isEqualTo(1);
        assertThat(bridges.getPage()).isZero();

        // filter by customer id not implemented yet
        bridges = bridgesService.getBridges("not-the-id", new QueryInfo(TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isZero();
        assertThat(bridges.getTotal()).isZero();
        assertThat(bridges.getPage()).isZero();
    }

    @Test
    public void testGetBridge() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        Bridge bridge = bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        //Wait for Workers to complete
        TestUtils.waitForBridgeToBeReady(bridgesService);

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNotNull();
        assertThat(retrievedBridge.getName()).isEqualTo(bridge.getName());
        assertThat(retrievedBridge.getCustomerId()).isEqualTo(bridge.getCustomerId());
        assertThat(retrievedBridge.getStatus()).isEqualTo(bridge.getStatus());
        assertThat(retrievedBridge.getShardId()).isEqualTo(TestConstants.SHARD_ID);
    }

    @Test
    public void testGetUnexistingBridge() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge("not-the-id", TestConstants.DEFAULT_CUSTOMER_ID));
    }

    @Test
    public void testGetBridgeWithWrongCustomerId() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge(bridge.getId(), "not-the-customerId"));
    }

    @Test
    public void testCreateBridge() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
        assertThat(bridge.getEndpoint()).isNull();

        ListResult<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID, new QueryInfo(TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isEqualTo(1);
    }

    @Test
    public void testUpdateBridgeStatus() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);

        // Emulate Shard setting Bridge status to PROVISIONING
        bridge.setStatus(ManagedResourceStatus.PROVISIONING);
        bridgesService.updateBridge(bridgesService.toDTO(bridge));

        assertThat(bridgesService.findByShardIdWithReadyDependencies(TestConstants.SHARD_ID)).isEmpty();

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge.getStatus()).isEqualTo(ManagedResourceStatus.PROVISIONING);
    }

    @Test
    public void testUpdateBridgeStatusReadyPublishedAt() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        // Emulate Shard setting Bridge status to PROVISIONING
        bridge.setStatus(ManagedResourceStatus.PROVISIONING);
        bridgesService.updateBridge(bridgesService.toDTO(bridge));

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge.getStatus()).isEqualTo(ManagedResourceStatus.PROVISIONING);
        assertThat(retrievedBridge.getPublishedAt()).isNull();

        // Once ready it should have its published date set
        bridge.setStatus(ManagedResourceStatus.READY);
        bridgesService.updateBridge(bridgesService.toDTO(bridge));

        Bridge publishedBridge = bridgesService.getBridge(bridge.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(publishedBridge.getStatus()).isEqualTo(ManagedResourceStatus.READY);
        ZonedDateTime publishedAt = publishedBridge.getPublishedAt();
        assertThat(publishedAt).isNotNull();

        //Check calls to set PublishedAt at idempotent
        bridgesService.updateBridge(bridgesService.toDTO(bridge));

        Bridge publishedBridge2 = bridgesService.getBridge(bridge.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(publishedBridge2.getStatus()).isEqualTo(ManagedResourceStatus.READY);
        assertThat(publishedBridge2.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    public void getBridge() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        Bridge found = bridgesService.getBridge(bridge.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(bridge.getId());
    }

    @Test
    public void getBridge_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge("foo"));
    }

    @Test
    public void testDeleteBridge() {
        Bridge bridge = createPersistBridge(ManagedResourceStatus.READY);

        bridgesService.deleteBridge(bridge.getId(), bridge.getCustomerId());

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), bridge.getCustomerId());
        assertThat(retrievedBridge.getStatus()).isEqualTo(ManagedResourceStatus.DEPROVISION);
    }

    @Test
    public void testDeleteBridge_whenStatusIsNotReady() {
        Bridge bridge = createPersistBridge(ManagedResourceStatus.PROVISIONING);
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> bridgesService.deleteBridge(bridge.getId(), bridge.getCustomerId()));
    }

    private Bridge createPersistBridge(ManagedResourceStatus status) {
        Bridge b = Fixtures.createBridge();
        b.setStatus(status);
        bridgeDAO.persist(b);
        return b;
    }
}
