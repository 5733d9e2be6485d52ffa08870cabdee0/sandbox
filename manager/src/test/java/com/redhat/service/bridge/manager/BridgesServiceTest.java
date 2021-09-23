package com.redhat.service.bridge.manager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.exceptions.ItemNotFoundException;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BridgesServiceTest {

    @Inject
    BridgesService bridgesService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testGetEmptyBridgesToDeploy() {
        List<Bridge> bridges = bridgesService.getBridgesByStatuses(Collections.singletonList(BridgeStatus.REQUESTED));
        Assertions.assertEquals(0, bridges.size());
    }

    @Test
    public void testGetEmptyBridges() {
        ListResult<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID, TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE);
        Assertions.assertEquals(0, bridges.getPage());
        Assertions.assertEquals(0, bridges.getTotal());
        Assertions.assertEquals(0, bridges.getSize());
    }

    @Test
    public void testGetBridges() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        ListResult<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID, TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE);
        Assertions.assertEquals(1, bridges.getSize());
        Assertions.assertEquals(1, bridges.getTotal());
        Assertions.assertEquals(0, bridges.getPage());

        // filter by customer id not implemented yet
        bridges = bridgesService.getBridges("not-the-id", TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE);
        Assertions.assertEquals(0, bridges.getSize());
        Assertions.assertEquals(0, bridges.getTotal());
        Assertions.assertEquals(0, bridges.getPage());
    }

    @Test
    public void testGetBridge() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        Bridge bridge = bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertNotNull(retrievedBridge);
        Assertions.assertEquals(bridge.getName(), retrievedBridge.getName());
        Assertions.assertEquals(bridge.getCustomerId(), retrievedBridge.getCustomerId());
        Assertions.assertEquals(bridge.getStatus(), retrievedBridge.getStatus());
    }

    @Test
    public void testGetUnexistingBridge() {
        Assertions.assertThrows(ItemNotFoundException.class, () -> bridgesService.getBridge("not-the-id", TestConstants.DEFAULT_CUSTOMER_ID));
    }

    @Test
    public void testGetBridgeWithWrongCustomerId() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        Bridge bridge = bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        Assertions.assertThrows(ItemNotFoundException.class, () -> bridgesService.getBridge(bridge.getId(), "not-the-customerId"));
    }

    @Test
    public void testCreateBridge() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        List<Bridge> bridgesToDeploy = bridgesService.getBridgesByStatuses(Collections.singletonList(BridgeStatus.REQUESTED));
        Assertions.assertEquals(1, bridgesToDeploy.size());
        Assertions.assertEquals(BridgeStatus.REQUESTED, bridgesToDeploy.get(0).getStatus());
        Assertions.assertNull(bridgesToDeploy.get(0).getEndpoint());

        ListResult<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID, TestConstants.DEFAULT_PAGE, TestConstants.DEFAULT_PAGE_SIZE);
        Assertions.assertEquals(1, bridges.getSize());
    }

    @Test
    public void testUpdateBridgeStatus() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        Bridge bridge = bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        List<Bridge> bridges = bridgesService.getBridgesByStatuses(Collections.singletonList(BridgeStatus.REQUESTED));
        Assertions.assertEquals(1, bridges.size());
        Assertions.assertEquals(BridgeStatus.REQUESTED, bridges.get(0).getStatus());

        bridge.setStatus(BridgeStatus.PROVISIONING);
        bridgesService.updateBridge(bridge.toDTO());

        bridges = bridgesService.getBridgesByStatuses(Collections.singletonList(BridgeStatus.REQUESTED));
        Assertions.assertEquals(0, bridges.size());

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertEquals(BridgeStatus.PROVISIONING, retrievedBridge.getStatus());
    }

    @Test
    public void getBridge() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        Bridge bridge = bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        Bridge found = bridgesService.getBridge(bridge.getId());
        Assertions.assertNotNull(found);
        Assertions.assertEquals(bridge.getId(), found.getId());
    }

    @Test
    public void getBridge_bridgeDoesNotExist() {
        Assertions.assertThrows(ItemNotFoundException.class, () -> bridgesService.getBridge("foo"));
    }
}
