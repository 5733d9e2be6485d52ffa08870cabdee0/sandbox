package com.redhat.developer.manager;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.api.models.requests.BridgeRequest;
import com.redhat.developer.manager.models.Bridge;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;

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
        List<Bridge> bridges = bridgesService.getBridgesToDeploy();
        Assertions.assertEquals(0, bridges.size());
    }

    @Test
    public void testGetEmptyBridges() {
        List<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertEquals(0, bridges.size());
    }

    @Test
    public void testGetBridges() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        List<Bridge> bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertEquals(1, bridges.size());

        // filter by customer id not implemented yet
        bridges = bridgesService.getBridges("not-the-id");
        Assertions.assertEquals(1, bridges.size());
    }

    @Test
    public void testCreateBridge() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        List<Bridge> bridges = bridgesService.getBridgesToDeploy();
        Assertions.assertEquals(1, bridges.size());
        Assertions.assertEquals(BridgeStatus.REQUESTED, bridges.get(0).getStatus());
        Assertions.assertNull(bridges.get(0).getEndpoint());

        bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertEquals(1, bridges.size());
    }

    @Test
    public void testUpdateBridgeStatus() {
        BridgeRequest request = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(TestConstants.DEFAULT_CUSTOMER_ID, request);

        List<Bridge> bridges = bridgesService.getBridgesToDeploy();
        Assertions.assertEquals(1, bridges.size());
        Assertions.assertEquals(BridgeStatus.REQUESTED, bridges.get(0).getStatus());

        Bridge bridge = bridges.get(0);
        bridge.setStatus(BridgeStatus.PROVISIONING);
        bridgesService.updateBridge(bridge);

        bridges = bridgesService.getBridgesToDeploy();
        Assertions.assertEquals(0, bridges.size());

        bridges = bridgesService.getBridges(TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertEquals(1, bridges.size());
        Assertions.assertEquals(BridgeStatus.PROVISIONING, bridges.get(0).getStatus());
    }
}
