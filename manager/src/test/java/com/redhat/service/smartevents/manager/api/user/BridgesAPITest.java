package com.redhat.service.smartevents.manager.api.user;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.api.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.api.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.utils.TestUtils;
import com.redhat.service.smartevents.manager.workers.WorkManager;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

import static com.redhat.service.smartevents.infra.api.APIConstants.USER_API_BASE_PATH;
import static com.redhat.service.smartevents.infra.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PROCESSOR_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.utils.TestUtils.createWebhookAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@QuarkusTest
public class BridgesAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    BridgeDAO bridgeDAO;

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    @SuppressWarnings("unused")
    RhoasService rhoasServiceMock;

    @InjectMock
    @SuppressWarnings("unused")
    // Effectively disable Work scheduling and execution without disabling Quarkus's Quartz.
    // Disabling Quarkus's Quartz leads to CDI injection issues as the Scheduler is not available.
    WorkManager workManager;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.SHARD_ID);
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.DEFAULT_ORGANISATION_ID);
        when(jwt.containsClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_USER_NAME);
        when(jwt.containsClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(true);
    }

    @Test
    public void testGetBridgesNoAuthentication() {
        TestUtils.getBridges().then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetEmptyBridges() {
        BridgeListResponse response = TestUtils.getBridges().as(BridgeListResponse.class);
        assertThat(response.getItems().size()).isZero();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createBridge() {
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME))
                .then().statusCode(202);
    }

    @Test
    public void createBridgeNoAuthentication() {
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME))
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createInvalidBridge() {
        TestUtils.createBridge(new BridgeRequest())
                .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getBridge() {
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME));
        bridgeCreateResponse.then().statusCode(202);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge).isNotNull();
        assertThat(retrievedBridge.getId()).isEqualTo(bridge.getId());
        assertThat(retrievedBridge.getName()).isEqualTo(bridge.getName());
        assertThat(retrievedBridge.getEndpoint()).isEqualTo(bridge.getEndpoint());
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getBridgeWithErrorHandler() {
        Action errorHandler = createWebhookAction();
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, errorHandler));
        bridgeCreateResponse.then().statusCode(202);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge).isNotNull();
        assertThat(retrievedBridge.getId()).isEqualTo(bridge.getId());
        assertThat(retrievedBridge.getName()).isEqualTo(bridge.getName());
        assertThat(retrievedBridge.getEndpoint()).isEqualTo(bridge.getEndpoint());
        assertThat(retrievedBridge.getErrorHandler()).isNotNull();
        assertThat(retrievedBridge.getErrorHandler()).isEqualTo(errorHandler);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getNonExistingBridge() {
        ErrorsResponse response = TestUtils.getBridge("not-the-id").then().statusCode(404).extract().as(ErrorsResponse.class);
        assertThat(response.getItems()).hasSize(1);

        ErrorResponse error = response.getItems().get(0);
        assertThat(error.getId()).isEqualTo("4");
        assertThat(error.getCode()).endsWith("4");
        assertThat(error.getReason()).isNotBlank();
    }

    @Test
    public void getBridgeNoAuthentication() {
        TestUtils.getBridge("any-id").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testCreateAndGetBridge() {
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME))
                .then().statusCode(202);

        BridgeListResponse bridgeListResponse = TestUtils.getBridges().as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(DEFAULT_BRIDGE_NAME);
        assertThat(bridgeResponse.getStatus()).isEqualTo(ACCEPTED);
        assertThat(bridgeResponse.getHref()).isEqualTo(USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();

        assertThat(bridgeResponse.getEndpoint()).isNull();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetBridgesFilterByName() {
        Bridge bridge1 = Fixtures.createBridge();
        bridge1.setName(DEFAULT_BRIDGE_NAME + "1");
        bridge1.setStatus(ACCEPTED);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createBridge();
        bridge2.setName(DEFAULT_BRIDGE_NAME + "2");
        bridge2.setStatus(ACCEPTED);
        bridgeDAO.persist(bridge2);

        BridgeListResponse bridgeListResponse = TestUtils.getBridgesFilterByName(DEFAULT_BRIDGE_NAME + "1").as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(bridge1.getName());
        assertThat(bridgeResponse.getStatus()).isEqualTo(bridge1.getStatus());
        assertThat(bridgeResponse.getHref()).isEqualTo(USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getEndpoint()).isNotNull();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetBridgesFilterByStatus() {
        Bridge bridge1 = Fixtures.createBridge();
        bridge1.setName(DEFAULT_BRIDGE_NAME + "1");
        bridge1.setStatus(READY);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createBridge();
        bridge2.setName(DEFAULT_BRIDGE_NAME + "2");
        bridge2.setStatus(ACCEPTED);
        bridgeDAO.persist(bridge2);

        BridgeListResponse bridgeListResponse = TestUtils.getBridgesFilterByStatus(ACCEPTED).as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(bridge2.getName());
        assertThat(bridgeResponse.getStatus()).isEqualTo(bridge2.getStatus());
        assertThat(bridgeResponse.getHref()).isEqualTo(USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getEndpoint()).isNotNull();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetBridgesFilterByMultipleStatuses() {
        Bridge bridge1 = Fixtures.createBridge();
        bridge1.setName(DEFAULT_BRIDGE_NAME + "1");
        bridge1.setStatus(READY);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createBridge();
        bridge2.setName(DEFAULT_BRIDGE_NAME + "2");
        bridge2.setStatus(ACCEPTED);
        bridgeDAO.persist(bridge2);

        BridgeListResponse bridgeListResponse = TestUtils.getBridgesFilterByStatus(READY, ACCEPTED).as(BridgeListResponse.class);

        // The default sorting is by submission date descending; so Bridge2 will be first
        assertThat(bridgeListResponse.getItems().size()).isEqualTo(2);
        BridgeResponse bridgeResponse1 = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse1.getName()).isEqualTo(bridge2.getName());
        assertThat(bridgeResponse1.getStatus()).isEqualTo(bridge2.getStatus());
        assertThat(bridgeResponse1.getHref()).isEqualTo(USER_API_BASE_PATH + bridgeResponse1.getId());
        assertThat(bridgeResponse1.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse1.getEndpoint()).isNotNull();

        BridgeResponse bridgeResponse2 = bridgeListResponse.getItems().get(1);
        assertThat(bridgeResponse2.getName()).isEqualTo(bridge1.getName());
        assertThat(bridgeResponse2.getStatus()).isEqualTo(bridge1.getStatus());
        assertThat(bridgeResponse2.getHref()).isEqualTo(USER_API_BASE_PATH + bridgeResponse2.getId());
        assertThat(bridgeResponse2.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse2.getEndpoint()).isNotNull();
    }

    @Test
    public void testGetBridgesFilterByStatusWithIncorrectValue() {
        // See JAX-RS 2.1 Specification, Section 3.2. 
        // HTTP-404 is correct if the QueryString contains an invalid value.
        // If the field or property is annotated with @MatrixParam, @QueryParam or @PathParam then an implementation
        // MUST generate an instance of NotFoundException (404 status) that wraps the thrown exception...
        TestUtils.getBridgesFilterByStatusWithAnyValue("banana").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetBridgesFilterByNameAndStatus() {
        Bridge bridge1 = Fixtures.createBridge();
        bridge1.setName(DEFAULT_BRIDGE_NAME + "1");
        bridge1.setStatus(READY);
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createBridge();
        bridge2.setName(DEFAULT_BRIDGE_NAME + "2");
        bridge2.setStatus(READY);
        bridgeDAO.persist(bridge2);

        BridgeListResponse bridgeListResponse = TestUtils.getBridgesFilterByNameAndStatus(DEFAULT_BRIDGE_NAME + "1", READY).as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(bridge1.getName());
        assertThat(bridgeResponse.getStatus()).isEqualTo(bridge1.getStatus());
        assertThat(bridgeResponse.getHref()).isEqualTo(USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getEndpoint()).isNotNull();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testDeleteBridge() {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(READY);
        bridgeDAO.persist(bridge);

        TestUtils.deleteBridge(bridge.getId()).then().statusCode(202);
        BridgeResponse response = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);

        assertThat(response.getStatus()).isEqualTo(ManagedResourceStatus.DEPROVISION);
    }

    @Test
    public void testDeleteBridgeNoAuthentication() {
        TestUtils.deleteBridge("any-id").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testDeleteNotExistingBridge() {
        TestUtils.deleteBridge("not-the-id").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testDeleteBridgeWithActiveProcessors() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        TestUtils.updateBridge(
                new BridgeDTO(bridgeResponse.getId(),
                        bridgeResponse.getName(),
                        bridgeResponse.getEndpoint(),
                        DEFAULT_CUSTOMER_ID,
                        DEFAULT_USER_NAME,
                        READY,
                        new KafkaConnectionDTO()));

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME, TestUtils.createKafkaAction())).then().statusCode(202);

        TestUtils.deleteBridge(bridgeResponse.getId()).then().statusCode(400);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testAlreadyExistingBridge() {
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME))
                .then().statusCode(202);
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME))
                .then().statusCode(400);
    }
}
