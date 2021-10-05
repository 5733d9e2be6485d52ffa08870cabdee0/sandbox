package com.redhat.service.bridge.manager.api.user;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

@QuarkusTest
public class BridgesAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testAuthentication() {
        TestUtils.getBridges().then().statusCode(401);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testGetEmptyBridges() {
        BridgeListResponse response = TestUtils.getBridges().as(BridgeListResponse.class);
        Assertions.assertEquals(0, response.getItems().size());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void createBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getBridge() {
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));
        bridgeCreateResponse.then().statusCode(201);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        Assertions.assertNotNull(retrievedBridge);
        Assertions.assertEquals(bridge.getId(), retrievedBridge.getId());
        Assertions.assertEquals(bridge.getName(), retrievedBridge.getName());
        Assertions.assertEquals(bridge.getEndpoint(), retrievedBridge.getEndpoint());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getUnexistingBridge() {
        TestUtils.getBridge("not-the-id").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testCreateAndGetBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);

        BridgeListResponse bridgeListResponse = TestUtils.getBridges().as(BridgeListResponse.class);

        Assertions.assertEquals(1, bridgeListResponse.getItems().size());
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        Assertions.assertEquals(TestConstants.DEFAULT_BRIDGE_NAME, bridgeResponse.getName());
        Assertions.assertEquals(BridgeStatus.REQUESTED, bridgeResponse.getStatus());
        Assertions.assertEquals(APIConstants.USER_API_BASE_PATH + bridgeResponse.getId(), bridgeResponse.getHref());
        Assertions.assertNotNull(bridgeResponse.getSubmittedAt());

        Assertions.assertNull(bridgeResponse.getEndpoint());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testDeleteBridge() {
        BridgeResponse response = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        TestUtils.deleteBridge(response.getId()).then().statusCode(202);
        response = TestUtils.getBridge(response.getId()).as(BridgeResponse.class);

        Assertions.assertEquals(BridgeStatus.DELETION_REQUESTED, response.getStatus());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testDeleteBridgeWithActiveProcessors() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        TestUtils.updateBridge(new BridgeDTO(bridgeResponse.getId(), bridgeResponse.getName(), bridgeResponse.getEndpoint(), TestConstants.DEFAULT_CUSTOMER_ID, BridgeStatus.AVAILABLE));

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(TestConstants.DEFAULT_PROCESSOR_NAME, TestUtils.createKafkaAction())).then().statusCode(201);

        TestUtils.deleteBridge(bridgeResponse.getId()).then().statusCode(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testAlreadyExistingBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(400);
    }
}
