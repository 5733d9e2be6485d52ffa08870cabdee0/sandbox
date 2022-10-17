package com.redhat.service.smartevents.manager.api.user;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.api.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.api.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorDAO;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.UnclassifiedConstraintViolationException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.InvalidCloudProviderException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.InvalidRegionException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersMissingException;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.ProcessorService;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.utils.TestUtils;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static com.redhat.service.smartevents.infra.api.APIConstants.USER_API_BASE_PATH;
import static com.redhat.service.smartevents.infra.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_TLS_CERTIFICATE;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_TLS_KEY;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_CLOUD_PROVIDER;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_ERROR_HANDLER_PROCESSOR_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PROCESSOR_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_REGION;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.utils.TestUtils.createKafkaAction;
import static com.redhat.service.smartevents.manager.utils.TestUtils.createWebhookAction;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@QuarkusTest
public class BridgesAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorService processorService;

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    @SuppressWarnings("unused")
    RhoasService rhoasServiceMock;

    @Inject
    BridgeErrorDAO errorDAO;

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
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(202);
    }

    @Test
    public void createBridgeNoAuthentication() {
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
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
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION));
        bridgeCreateResponse.then().statusCode(202);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge).isNotNull();
        assertThat(retrievedBridge.getId()).isEqualTo(bridge.getId());
        assertThat(retrievedBridge.getName()).isEqualTo(bridge.getName());
        assertThat(retrievedBridge.getEndpoint()).isEqualTo(bridge.getEndpoint());
        assertThat(retrievedBridge.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(retrievedBridge.getRegion()).isEqualTo(DEFAULT_REGION);
        assertThat(retrievedBridge.getErrorHandler()).isNull();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getBridgeWithErrorHandler() {
        Action errorHandler = createWebhookAction();
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, errorHandler));
        bridgeCreateResponse.then().statusCode(202);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge).isNotNull();
        assertThat(retrievedBridge.getId()).isEqualTo(bridge.getId());
        assertThat(retrievedBridge.getName()).isEqualTo(bridge.getName());
        assertThat(retrievedBridge.getEndpoint()).isEqualTo(bridge.getEndpoint());
        assertThat(retrievedBridge.getErrorHandler()).isNotNull();
        assertThat(retrievedBridge.getErrorHandler()).isEqualTo(errorHandler);
        assertThat(retrievedBridge.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(retrievedBridge.getRegion()).isEqualTo(DEFAULT_REGION);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getBridgeWithNoNameWithIncompleteErrorHandler() {
        Action incompleteErrorHandler = new Action();
        incompleteErrorHandler.setType(WebhookAction.TYPE);
        ErrorsResponse errorsResponse = TestUtils.createBridge(new BridgeRequest("",
                DEFAULT_CLOUD_PROVIDER,
                DEFAULT_REGION,
                incompleteErrorHandler))
                .as(ErrorsResponse.class);

        // Missing name and Incomplete Error Handler definition
        assertErrorResponses(errorsResponse, Set.of(UnclassifiedConstraintViolationException.class, ProcessorGatewayParametersMissingException.class));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getBridgeWithDuplicateNameWithIncompleteErrorHandler() {
        // Create the first Bridge successfully.
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(202);

        // Create another Bridge with the same name but incomplete Error Handler definition.
        Action incompleteErrorHandler = new Action();
        incompleteErrorHandler.setType(WebhookAction.TYPE);
        ErrorsResponse errorsResponse1 = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME,
                DEFAULT_CLOUD_PROVIDER,
                DEFAULT_REGION,
                incompleteErrorHandler))
                .as(ErrorsResponse.class);

        // Incomplete Error Handler definition. Note the duplicate name is NOT detected with this configuration.
        // See https://issues.redhat.com/browse/MGDOBR-947
        assertErrorResponses(errorsResponse1, Set.of(ProcessorGatewayParametersMissingException.class));

        // Try creating the other Bridge again with the same name and complete Error Handler definition.
        Action completeErrorHandler = TestUtils.createWebhookAction();
        ErrorsResponse errorsResponse2 = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME,
                DEFAULT_CLOUD_PROVIDER,
                DEFAULT_REGION,
                completeErrorHandler))
                .as(ErrorsResponse.class);

        // Now the duplicate name IS detected and reported.
        // See https://issues.redhat.com/browse/MGDOBR-947
        assertErrorResponses(errorsResponse2, Set.of(AlreadyExistingItemException.class));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createBridge_withInvalidCloudProvider() {
        ErrorsResponse errorsResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, "dodgyCloudProvider", DEFAULT_REGION))
                .as(ErrorsResponse.class);

        assertErrorResponses(errorsResponse, Set.of(InvalidCloudProviderException.class, InvalidRegionException.class));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createBridge_withInvalidRegion() {
        ErrorsResponse errorsResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, "dodgyRegion"))
                .as(ErrorsResponse.class);

        assertErrorResponses(errorsResponse, Set.of(InvalidRegionException.class));
    }

    private void assertErrorResponses(ErrorsResponse errorsResponse, Set<Class<? extends RuntimeException>> exceptions) {
        Set<String> expectedErrorCodes = exceptions.stream().map(e -> errorDAO.findByException(e).getCode()).collect(Collectors.toSet());

        assertThat(errorsResponse.getItems())
                .hasSize(expectedErrorCodes.size())
                .map(ErrorResponse::getCode)
                .allSatisfy(expectedErrorCodes::contains);
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
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(202);

        BridgeListResponse bridgeListResponse = TestUtils.getBridges().as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(DEFAULT_BRIDGE_NAME);
        assertThat(bridgeResponse.getStatus()).isEqualTo(ACCEPTED);
        assertThat(bridgeResponse.getHref()).isEqualTo(USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse.getRegion()).isEqualTo(DEFAULT_REGION);
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
        assertThat(bridgeResponse.getEndpoint()).isNull();
        assertThat(bridgeResponse.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse.getRegion()).isEqualTo(DEFAULT_REGION);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    // See https://issues.redhat.com/browse/MGDOBR-1113
    public void testGetBridgesFilterByNameWithCreationByApi() {
        BridgeRequest bridge1 = new BridgeRequest(DEFAULT_BRIDGE_NAME + "1", DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        BridgeRequest bridge2 = new BridgeRequest(DEFAULT_BRIDGE_NAME + "2", DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION);
        TestUtils.createBridge(bridge1);
        TestUtils.createBridge(bridge2);

        BridgeListResponse bridgeListResponse = TestUtils.getBridgesFilterByName(DEFAULT_BRIDGE_NAME + "1").as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(bridge1.getName());
        assertThat(bridgeResponse.getStatus()).isEqualTo(ACCEPTED);
        assertThat(bridgeResponse.getHref()).isEqualTo(USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getEndpoint()).isNull();
        assertThat(bridgeResponse.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse.getRegion()).isEqualTo(DEFAULT_REGION);
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
        assertThat(bridgeResponse.getEndpoint()).isNull();
        assertThat(bridgeResponse.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse.getRegion()).isEqualTo(DEFAULT_REGION);
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
        assertThat(bridgeResponse1.getEndpoint()).isNull();
        assertThat(bridgeResponse1.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse1.getRegion()).isEqualTo(DEFAULT_REGION);

        BridgeResponse bridgeResponse2 = bridgeListResponse.getItems().get(1);
        assertThat(bridgeResponse2.getName()).isEqualTo(bridge1.getName());
        assertThat(bridgeResponse2.getStatus()).isEqualTo(bridge1.getStatus());
        assertThat(bridgeResponse2.getHref()).isEqualTo(USER_API_BASE_PATH + bridgeResponse2.getId());
        assertThat(bridgeResponse2.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse2.getEndpoint()).isNotNull();
        assertThat(bridgeResponse2.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse2.getRegion()).isEqualTo(DEFAULT_REGION);
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
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);
        TestUtils.updateBridge(
                new BridgeDTO(bridgeResponse.getId(),
                        bridgeResponse.getName(),
                        bridgeResponse.getEndpoint(),
                        null,
                        null,
                        DEFAULT_CUSTOMER_ID,
                        DEFAULT_USER_NAME,
                        READY,
                        new KafkaConnectionDTO()));

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME, TestUtils.createKafkaAction())).then().statusCode(202);

        TestUtils.deleteBridge(bridgeResponse.getId()).then().statusCode(400);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testDeleteBridgeWithActiveErrorHandler() {
        Action errorHandler = createWebhookAction();
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, errorHandler)).as(BridgeResponse.class);
        TestUtils.updateBridge(
                new BridgeDTO(bridgeResponse.getId(),
                        bridgeResponse.getName(),
                        bridgeResponse.getEndpoint(),
                        DEFAULT_BRIDGE_TLS_CERTIFICATE,
                        DEFAULT_BRIDGE_TLS_KEY,
                        DEFAULT_CUSTOMER_ID,
                        DEFAULT_USER_NAME,
                        READY,
                        new KafkaConnectionDTO()));

        // The call to create a Bridge above is stubbed to be a NOP as WorkManager is mocked.
        // Therefore, manually create the Error Handler records that would have otherwise existed.
        createErrorHandler(bridgeResponse, errorHandler, READY);

        TestUtils.deleteBridge(bridgeResponse.getId()).then().statusCode(202);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testDeleteBridgeWithActiveErrorHandlerWhenBothFailed() {
        Action errorHandler = createWebhookAction();
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, errorHandler)).as(BridgeResponse.class);
        TestUtils.updateBridge(
                new BridgeDTO(bridgeResponse.getId(),
                        bridgeResponse.getName(),
                        bridgeResponse.getEndpoint(),
                        DEFAULT_BRIDGE_TLS_CERTIFICATE,
                        DEFAULT_BRIDGE_TLS_KEY,
                        DEFAULT_CUSTOMER_ID,
                        DEFAULT_USER_NAME,
                        FAILED,
                        new KafkaConnectionDTO()));

        // The call to create a Bridge above is stubbed to be a NOP as WorkManager is mocked.
        // Therefore, manually create the Error Handler records that would have otherwise existed.
        createErrorHandler(bridgeResponse, errorHandler, FAILED);

        TestUtils.deleteBridge(bridgeResponse.getId()).then().statusCode(202);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testAlreadyExistingBridge() {
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(202);
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateBridgeAddErrorHandler() {
        //Create Bridge without an ErrorHandler
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION));
        bridgeCreateResponse.then().statusCode(202);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge.getErrorHandler()).isNull();

        // Place Bridge into READY state to be able to update
        TestUtils.updateBridge(
                new BridgeDTO(bridge.getId(),
                        bridge.getName(),
                        bridge.getEndpoint(),
                        DEFAULT_BRIDGE_TLS_CERTIFICATE,
                        DEFAULT_BRIDGE_TLS_KEY,
                        DEFAULT_CUSTOMER_ID,
                        DEFAULT_USER_NAME,
                        READY,
                        new KafkaConnectionDTO()));

        //Update Bridge removing ErrorHandler
        Action errorHandler = createWebhookAction();
        Response bridgeUpdateResponse = TestUtils.updateBridge(bridge.getId(), new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, errorHandler));
        bridgeUpdateResponse.then().statusCode(202);

        BridgeResponse updatedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(updatedBridge.getErrorHandler()).isNotNull();
        assertThat(updatedBridge.getErrorHandler()).isEqualTo(errorHandler);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateBridgeRemoveErrorHandler() {
        //Create Bridge with an ErrorHandler
        Action errorHandler = createWebhookAction();
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, errorHandler));
        bridgeCreateResponse.then().statusCode(202);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge.getErrorHandler()).isNotNull();
        assertThat(retrievedBridge.getErrorHandler()).isEqualTo(errorHandler);

        // Place Bridge into READY state to be able to update
        TestUtils.updateBridge(
                new BridgeDTO(bridge.getId(),
                        bridge.getName(),
                        bridge.getEndpoint(),
                        DEFAULT_BRIDGE_TLS_CERTIFICATE,
                        DEFAULT_BRIDGE_TLS_KEY,
                        DEFAULT_CUSTOMER_ID,
                        DEFAULT_USER_NAME,
                        READY,
                        new KafkaConnectionDTO()));

        //Update Bridge removing ErrorHandler
        Response bridgeUpdateResponse = TestUtils.updateBridge(bridge.getId(), new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION));
        bridgeUpdateResponse.then().statusCode(202);

        BridgeResponse updatedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(updatedBridge.getErrorHandler()).isNull();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateBridgeUpdateErrorHandler() {
        //Create Bridge with an ErrorHandler
        Action errorHandler1 = createWebhookAction();
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, errorHandler1));
        bridgeCreateResponse.then().statusCode(202);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge.getErrorHandler()).isNotNull();
        assertThat(retrievedBridge.getErrorHandler()).isEqualTo(errorHandler1);

        // Place Bridge into READY state to be able to update
        TestUtils.updateBridge(
                new BridgeDTO(bridge.getId(),
                        bridge.getName(),
                        bridge.getEndpoint(),
                        DEFAULT_BRIDGE_TLS_CERTIFICATE,
                        DEFAULT_BRIDGE_TLS_KEY,
                        DEFAULT_CUSTOMER_ID,
                        DEFAULT_USER_NAME,
                        READY,
                        new KafkaConnectionDTO()));

        //Update Bridge removing ErrorHandler
        Action errorHandler2 = createWebhookAction();
        errorHandler2.setMapParameters(Map.of(WebhookAction.ENDPOINT_PARAM, "https://webhook.site/updated-error-handler"));
        Response bridgeUpdateResponse = TestUtils.updateBridge(bridge.getId(), new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, errorHandler2));
        bridgeUpdateResponse.then().statusCode(202);

        BridgeResponse updatedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(updatedBridge.getErrorHandler()).isNotNull();
        assertThat(updatedBridge.getErrorHandler()).isEqualTo(errorHandler2);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateBridgeUpdateErrorHandlerTypeWebhookToKafka() {
        doUpdateBridgeUpdateErrorHandlerType(createWebhookAction(), createKafkaAction());
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateBridgeUpdateErrorHandlerTypeKafkaToWebhook() {
        doUpdateBridgeUpdateErrorHandlerType(createKafkaAction(), createWebhookAction());
    }

    private void doUpdateBridgeUpdateErrorHandlerType(Action errorHandler1, Action errorHandler2) {
        //Create Bridge with an ErrorHandler
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, errorHandler1));
        bridgeCreateResponse.then().statusCode(202);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge.getErrorHandler()).isNotNull();
        assertThat(retrievedBridge.getErrorHandler()).isEqualTo(errorHandler1);

        // Place Bridge into READY state to be able to update
        TestUtils.updateBridge(
                new BridgeDTO(bridge.getId(),
                        bridge.getName(),
                        bridge.getEndpoint(),
                        DEFAULT_BRIDGE_TLS_CERTIFICATE,
                        DEFAULT_BRIDGE_TLS_KEY,
                        DEFAULT_CUSTOMER_ID,
                        DEFAULT_USER_NAME,
                        READY,
                        new KafkaConnectionDTO()));

        //Update Bridge removing ErrorHandler
        Response bridgeUpdateResponse = TestUtils.updateBridge(bridge.getId(), new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION, errorHandler2));
        bridgeUpdateResponse.then().statusCode(202);

        BridgeResponse updatedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(updatedBridge.getErrorHandler()).isNotNull();
        assertThat(updatedBridge.getErrorHandler()).isEqualTo(errorHandler2);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void updateBridgeUpdateNameNotSupported() {
        //Create Bridge
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION));
        bridgeCreateResponse.then().statusCode(202);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(retrievedBridge.getName()).isEqualTo(DEFAULT_BRIDGE_NAME);

        // Place Bridge into READY state to be able to update
        TestUtils.updateBridge(
                new BridgeDTO(bridge.getId(),
                        bridge.getName(),
                        bridge.getEndpoint(),
                        DEFAULT_BRIDGE_TLS_CERTIFICATE,
                        DEFAULT_BRIDGE_TLS_KEY,
                        DEFAULT_CUSTOMER_ID,
                        DEFAULT_USER_NAME,
                        READY,
                        new KafkaConnectionDTO()));

        //Attempt to update Bridge name
        Response bridgeUpdateResponse = TestUtils.updateBridge(bridge.getId(), new BridgeRequest(DEFAULT_BRIDGE_NAME + "-updated", DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION));
        bridgeUpdateResponse.then().statusCode(400);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testBasicMetricsForBridges() {
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(202);

        String metrics = given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/q/metrics")
                .then()
                .extract()
                .body()
                .asString();

        assertThat(metrics).isNotNull();
        assertThat(metrics)
                .contains("http_server_requests_seconds_count{method=\"POST\",outcome=\"SUCCESS\",status=\"202\",uri=\"/api/smartevents_mgmt/v1/bridges\",} 1.0")
                .contains("http_server_requests_seconds_sum{method=\"POST\",outcome=\"SUCCESS\",status=\"202\",uri=\"/api/smartevents_mgmt/v1/bridges\",}");
    }

    private void createErrorHandler(BridgeResponse bridgeResponse, Action errorHandler, ManagedResourceStatus errorHandlerStatus) {
        Bridge bridge = bridgeDAO.findById(bridgeResponse.getId());
        ProcessorRequest processorRequest = new ProcessorRequest(DEFAULT_ERROR_HANDLER_PROCESSOR_NAME, errorHandler);
        Processor processor = processorService.createErrorHandlerProcessor(bridge.getId(), DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, processorRequest);
        processor.setStatus(errorHandlerStatus);
    }

}
