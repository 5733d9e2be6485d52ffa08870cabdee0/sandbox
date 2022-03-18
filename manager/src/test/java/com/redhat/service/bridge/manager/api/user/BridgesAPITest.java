package com.redhat.service.bridge.manager.api.user;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.api.models.responses.ErrorResponse;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.WorkerSchedulerProfile;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(WorkerSchedulerProfile.class)
public class BridgesAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    RhoasService rhoasServiceMock;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        when(jwt.getClaim(APIConstants.SUBJECT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.SHARD_ID);
    }

    @Test
    public void testAuthentication() {
        TestUtils.getBridges().then().statusCode(401);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testGetEmptyBridges() {
        BridgeListResponse response = TestUtils.getBridges().as(BridgeListResponse.class);
        assertThat(response.getItems().size()).isZero();
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void createBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(202);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getBridge() {
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));
        bridgeCreateResponse.then().statusCode(202);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge).isNotNull();
        assertThat(retrievedBridge.getId()).isEqualTo(bridge.getId());
        assertThat(retrievedBridge.getName()).isEqualTo(bridge.getName());
        assertThat(retrievedBridge.getEndpoint()).isEqualTo(bridge.getEndpoint());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getUnexistingBridge() {
        ErrorResponse response = TestUtils.getBridge("not-the-id").then().statusCode(404).extract().as(ErrorResponse.class);
        assertThat(response.getId()).isEqualTo("4");
        assertThat(response.getCode()).endsWith("4");
        assertThat(response.getReason()).isNotBlank();
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testCreateAndGetBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(202);

        BridgeListResponse bridgeListResponse = TestUtils.getBridges().as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(TestConstants.DEFAULT_BRIDGE_NAME);
        assertThat(bridgeResponse.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
        assertThat(bridgeResponse.getHref()).isEqualTo(APIConstants.USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();

        assertThat(bridgeResponse.getEndpoint()).isNull();
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testDeleteBridge() {
        BridgeResponse response = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        TestUtils.deleteBridge(response.getId()).then().statusCode(202);
        response = TestUtils.getBridge(response.getId()).as(BridgeResponse.class);

        assertThat(response.getStatus()).isEqualTo(ManagedResourceStatus.DEPROVISION);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testDeleteBridgeWithActiveProcessors() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        TestUtils.updateBridge(
                new BridgeDTO(bridgeResponse.getId(), bridgeResponse.getName(), bridgeResponse.getEndpoint(), TestConstants.DEFAULT_CUSTOMER_ID, ManagedResourceStatus.READY,
                        new KafkaConnectionDTO()));

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(TestConstants.DEFAULT_PROCESSOR_NAME, TestUtils.createKafkaAction())).then().statusCode(202);

        TestUtils.deleteBridge(bridgeResponse.getId()).then().statusCode(400);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testAlreadyExistingBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(202);
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(400);
    }
}
