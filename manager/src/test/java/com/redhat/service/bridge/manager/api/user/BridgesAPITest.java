package com.redhat.service.bridge.manager.api.user;

import javax.inject.Inject;

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
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class BridgesAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    void testGetEmptyBridges() {
        BridgeListResponse response = TestUtils.getBridges().as(BridgeListResponse.class);
        assertThat(response.getItems().size()).isZero();
    }

    @Test
    void createBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);
    }

    @Test
    void getBridge() {
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));
        bridgeCreateResponse.then().statusCode(201);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge).isNotNull();
        assertThat(retrievedBridge.getId()).isEqualTo(bridge.getId());
        assertThat(retrievedBridge.getName()).isEqualTo(bridge.getName());
        assertThat(retrievedBridge.getEndpoint()).isEqualTo(bridge.getEndpoint());
    }

    @Test
    void getUnexistingBridge() {
        TestUtils.getBridge("not-the-id").then().statusCode(404);
    }

    @Test
    void testCreateAndGetBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);

        BridgeListResponse bridgeListResponse = TestUtils.getBridges().as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(TestConstants.DEFAULT_BRIDGE_NAME);
        assertThat(bridgeResponse.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(bridgeResponse.getHref()).isEqualTo(APIConstants.USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();

        assertThat(bridgeResponse.getEndpoint()).isNull();
    }

    @Test
    void testDeleteBridge() {
        BridgeResponse response = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        TestUtils.deleteBridge(response.getId()).then().statusCode(202);
        response = TestUtils.getBridge(response.getId()).as(BridgeResponse.class);

        assertThat(response.getStatus()).isEqualTo(BridgeStatus.DELETION_REQUESTED);
    }

    @Test
    void testDeleteBridgeWithActiveProcessors() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        TestUtils.updateBridge(new BridgeDTO(bridgeResponse.getId(), bridgeResponse.getName(), bridgeResponse.getEndpoint(), TestConstants.DEFAULT_CUSTOMER_ID, BridgeStatus.AVAILABLE));

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(TestConstants.DEFAULT_PROCESSOR_NAME, TestUtils.createKafkaAction())).then().statusCode(201);

        TestUtils.deleteBridge(bridgeResponse.getId()).then().statusCode(400);
    }

    @Test
    void testAlreadyExistingBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(400);
    }
}
