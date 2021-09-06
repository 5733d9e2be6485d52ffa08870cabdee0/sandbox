package com.redhat.developer.manager.api.user;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.TestConstants;
import com.redhat.developer.manager.api.models.requests.BridgeRequest;
import com.redhat.developer.manager.api.models.responses.BridgeListResponse;
import com.redhat.developer.manager.api.models.responses.BridgeResponse;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;
import com.redhat.developer.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BridgesAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testGetEmptyBridges() {
        BridgeListResponse response = TestUtils.getBridges().as(BridgeListResponse.class);
        Assertions.assertEquals(0, response.getItems().size());
    }

    @Test
    public void createBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(200);
    }

    @Test
    public void testCreateAndGetBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(200);

        BridgeListResponse bridgeListResponse = TestUtils.getBridges().as(BridgeListResponse.class);

        Assertions.assertEquals(1, bridgeListResponse.getItems().size());
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        Assertions.assertEquals(TestConstants.DEFAULT_BRIDGE_NAME, bridgeResponse.getName());
        Assertions.assertEquals(BridgeStatus.REQUESTED, bridgeResponse.getStatus());
        Assertions.assertNotNull(bridgeResponse.getSubmittedAt());

        Assertions.assertNull(bridgeResponse.getEndpoint());
    }

    @Test
    public void testAlreadyExistingBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(200);
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(400);
    }
}
