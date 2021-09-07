package com.redhat.developer.manager.api.internal;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.TestConstants;
import com.redhat.developer.manager.api.models.requests.BridgeRequest;
import com.redhat.developer.manager.api.models.responses.BridgeResponse;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;
import com.redhat.developer.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class ShardBridgesSyncAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testGetEmptyBridgesToDeploy() {
        List<BridgeDTO> response = TestUtils.getBridgesToDeploy().as(new TypeRef<List<BridgeDTO>>() {
        });
        Assertions.assertEquals(0, response.size());
    }

    @Test
    public void testGetBridgesToDeploy() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        List<BridgeDTO> response = TestUtils.getBridgesToDeploy().as(new TypeRef<List<BridgeDTO>>() {
        });

        Assertions.assertEquals(1, response.size());
        BridgeDTO bridge = response.get(0);
        Assertions.assertEquals(TestConstants.DEFAULT_BRIDGE_NAME, bridge.getName());
        Assertions.assertEquals(TestConstants.DEFAULT_CUSTOMER_ID, bridge.getCustomerId());
        Assertions.assertEquals(BridgeStatus.REQUESTED, bridge.getStatus());
        Assertions.assertNull(bridge.getEndpoint());
    }

    @Test
    public void testGetBridgesToDelete() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));
        List<BridgeDTO> bridgesToDeploy = TestUtils.getBridgesToDeploy().as(new TypeRef<List<BridgeDTO>>() {
        });
        BridgeDTO bridge = bridgesToDeploy.get(0);

        TestUtils.deleteBridge(bridge.getId()).then().statusCode(202);
        bridgesToDeploy = TestUtils.getBridgesToDeploy().as(new TypeRef<List<BridgeDTO>>() {
        });
        List<BridgeDTO> bridgesToDelete = TestUtils.getBridgesToDelete().as(new TypeRef<List<BridgeDTO>>() {
        });

        Assertions.assertEquals(0, bridgesToDeploy.size());
        Assertions.assertEquals(1, bridgesToDelete.size());
    }

    @Test
    public void testNotifyDeployment() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        List<BridgeDTO> bridgesToDeploy = TestUtils.getBridgesToDeploy().as(new TypeRef<List<BridgeDTO>>() {
        });

        Assertions.assertEquals(1, bridgesToDeploy.size());

        BridgeDTO bridge = bridgesToDeploy.get(0);
        bridge.setStatus(BridgeStatus.PROVISIONING);
        TestUtils.updateBridge(bridge).then().statusCode(200);

        bridgesToDeploy = TestUtils.getBridgesToDeploy().as(new TypeRef<List<BridgeDTO>>() {
        });

        Assertions.assertEquals(0, bridgesToDeploy.size());
    }

    @Test
    public void testNotifyDeletion() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        List<BridgeDTO> bridgesToDeploy = TestUtils.getBridgesToDeploy().as(new TypeRef<List<BridgeDTO>>() {
        });
        BridgeDTO bridge = bridgesToDeploy.get(0);

        TestUtils.deleteBridge(bridge.getId()).then().statusCode(202);

        BridgeResponse bridgeResponse = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        Assertions.assertEquals(BridgeStatus.DELETION_REQUESTED, bridgeResponse.getStatus());

        bridge.setStatus(BridgeStatus.DELETED);
        TestUtils.updateBridge(bridge).then().statusCode(200);

        TestUtils.getBridge(bridge.getId()).then().statusCode(400);
    }
}
