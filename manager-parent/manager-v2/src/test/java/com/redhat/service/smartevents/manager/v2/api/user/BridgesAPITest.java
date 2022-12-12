package com.redhat.service.smartevents.manager.v2.api.user;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.api.APIConstants;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorDAO;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.InvalidCloudProviderException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.InvalidRegionException;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeListResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.manager.v2.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CLOUD_PROVIDER;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_REGION;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@QuarkusTest
public class BridgesAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    BridgeErrorDAO errorDAO;

    @InjectMock
    JsonWebToken jwt;

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
        TestUtils.listBridges().then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetEmptyBridges() {
        BridgeListResponse response = TestUtils.listBridges().as(BridgeListResponse.class);
        assertThat(response.getItems().size()).isZero();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createBridge() {
        Response response = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION));

        response.then().statusCode(202);
        BridgeResponse bridgeResponse = response.as(BridgeResponse.class);
        assertThat(bridgeResponse.getStatus()).isEqualTo(ACCEPTED);
        assertThat(bridgeResponse.getId()).isNotNull();
        assertThat(bridgeResponse.getName()).isEqualTo(DEFAULT_BRIDGE_NAME);
        assertThat(bridgeResponse.getEndpoint()).isNull();
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getPublishedAt()).isNull();
        assertThat(bridgeResponse.getModifiedAt()).isNull();
        assertThat(bridgeResponse.getHref()).contains(bridgeResponse.getId());
        assertThat(bridgeResponse.getOwner()).isNotNull();
        assertThat(bridgeResponse.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse.getRegion()).isEqualTo(DEFAULT_REGION);
        assertThat(bridgeResponse.getStatusMessage()).isNull();
    }

    @Test
    public void createBridgeNoAuthentication() {
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createBridgeOrganisationWithNoQuota() {
        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn("organisation-with-no-quota");
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(402).body("kind", Matchers.equalTo("Errors"));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createInvalidBridge() {
        TestUtils.createBridge(new BridgeRequest())
                .then().statusCode(400);
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

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testCreateAndGetBridge() {
        TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION))
                .then().statusCode(202);

        BridgeListResponse bridgeListResponse = TestUtils.listBridges().as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(DEFAULT_BRIDGE_NAME);
        assertThat(bridgeResponse.getStatus()).isEqualTo(ACCEPTED);
        assertThat(bridgeResponse.getHref()).isEqualTo(V2APIConstants.V2_USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse.getRegion()).isEqualTo(DEFAULT_REGION);
        assertThat(bridgeResponse.getEndpoint()).isNull();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetBridgesFilterByName() {
        Bridge bridge1 = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_ID + "1", DEFAULT_BRIDGE_NAME + "1");
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_ID + "2", DEFAULT_BRIDGE_NAME + "2");
        bridgeDAO.persist(bridge2);

        BridgeListResponse bridgeListResponse = TestUtils.listBridgesFilterByName(DEFAULT_BRIDGE_NAME + "1").as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(bridge1.getName());
        assertThat(bridgeResponse.getStatus()).isEqualTo(ACCEPTED);
        assertThat(bridgeResponse.getHref()).isEqualTo(V2APIConstants.V2_USER_API_BASE_PATH + bridgeResponse.getId());
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

        BridgeListResponse bridgeListResponse = TestUtils.listBridgesFilterByName(DEFAULT_BRIDGE_NAME + "1").as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(bridge1.getName());
        assertThat(bridgeResponse.getStatus()).isEqualTo(ACCEPTED);
        assertThat(bridgeResponse.getHref()).isEqualTo(V2APIConstants.V2_USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getEndpoint()).isNull();
        assertThat(bridgeResponse.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse.getRegion()).isEqualTo(DEFAULT_REGION);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetBridgesFilterByStatus() {
        Bridge bridge1 = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID + "1", DEFAULT_BRIDGE_NAME + "1");
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_ID + "2", DEFAULT_BRIDGE_NAME + "2");
        bridgeDAO.persist(bridge2);

        BridgeListResponse bridgeListResponse = TestUtils.listBridgesFilterByStatus(ACCEPTED).as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(bridge2.getName());
        assertThat(bridgeResponse.getStatus()).isEqualTo(ACCEPTED);
        assertThat(bridgeResponse.getHref()).isEqualTo(V2APIConstants.V2_USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getEndpoint()).isNull();
        assertThat(bridgeResponse.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse.getRegion()).isEqualTo(DEFAULT_REGION);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetBridgesFilterByMultipleStatuses() {
        Bridge bridge1 = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID + "1", DEFAULT_BRIDGE_NAME + "1");
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_ID + "2", DEFAULT_BRIDGE_NAME + "2");
        bridgeDAO.persist(bridge2);

        BridgeListResponse bridgeListResponse = TestUtils.listBridgesFilterByStatus(READY, ACCEPTED).as(BridgeListResponse.class);

        // The default sorting is by submission date descending; so Bridge2 will be first
        assertThat(bridgeListResponse.getItems().size()).isEqualTo(2);
        BridgeResponse bridgeResponse1 = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse1.getName()).isEqualTo(bridge2.getName());
        assertThat(bridgeResponse1.getStatus()).isEqualTo(ACCEPTED);
        assertThat(bridgeResponse1.getHref()).isEqualTo(V2APIConstants.V2_USER_API_BASE_PATH + bridgeResponse1.getId());
        assertThat(bridgeResponse1.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse1.getEndpoint()).isNull();
        assertThat(bridgeResponse1.getCloudProvider()).isEqualTo(DEFAULT_CLOUD_PROVIDER);
        assertThat(bridgeResponse1.getRegion()).isEqualTo(DEFAULT_REGION);

        BridgeResponse bridgeResponse2 = bridgeListResponse.getItems().get(1);
        assertThat(bridgeResponse2.getName()).isEqualTo(bridge1.getName());
        assertThat(bridgeResponse2.getStatus()).isEqualTo(READY);
        assertThat(bridgeResponse2.getHref()).isEqualTo(V2APIConstants.V2_USER_API_BASE_PATH + bridgeResponse2.getId());
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
        TestUtils.listBridgesFilterByStatusWithAnyValue("banana").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testGetBridgesFilterByNameAndStatus() {
        Bridge bridge1 = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID + "1", DEFAULT_BRIDGE_NAME + "1");
        bridgeDAO.persist(bridge1);

        Bridge bridge2 = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID + "2", DEFAULT_BRIDGE_NAME + "2");
        bridgeDAO.persist(bridge2);

        BridgeListResponse bridgeListResponse = TestUtils.listBridgesFilterByNameAndStatus(DEFAULT_BRIDGE_NAME + "1", READY).as(BridgeListResponse.class);

        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        assertThat(bridgeResponse.getName()).isEqualTo(bridge1.getName());
        assertThat(bridgeResponse.getStatus()).isEqualTo(READY);
        assertThat(bridgeResponse.getHref()).isEqualTo(V2APIConstants.V2_USER_API_BASE_PATH + bridgeResponse.getId());
        assertThat(bridgeResponse.getSubmittedAt()).isNotNull();
        assertThat(bridgeResponse.getEndpoint()).isNotNull();
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testPagination() {
        int totalBridges = 15;
        for (int i = 0; i < totalBridges; i++) {
            Bridge b = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID + i, DEFAULT_BRIDGE_NAME + i);
            bridgeDAO.persist(b);
        }

        BridgeListResponse bridgeListResponse = TestUtils.listBridges(0, 1).as(BridgeListResponse.class);
        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        assertThat(bridgeListResponse.getTotal()).isEqualTo(totalBridges);
        assertThat(bridgeListResponse.getPage()).isEqualTo(0);
        // Most recent first
        assertThat(bridgeListResponse.getItems().get(0).getName()).isEqualTo(DEFAULT_BRIDGE_NAME + "14");

        bridgeListResponse = TestUtils.listBridges(14, 1).as(BridgeListResponse.class);
        assertThat(bridgeListResponse.getItems().size()).isEqualTo(1);
        assertThat(bridgeListResponse.getTotal()).isEqualTo(totalBridges);
        assertThat(bridgeListResponse.getPage()).isEqualTo(14);
        assertThat(bridgeListResponse.getItems().get(0).getName()).isEqualTo(DEFAULT_BRIDGE_NAME + "0");

        bridgeListResponse = TestUtils.listBridges(15, 1).as(BridgeListResponse.class);
        assertThat(bridgeListResponse.getItems().size()).isEqualTo(0);
        assertThat(bridgeListResponse.getTotal()).isEqualTo(totalBridges);

        bridgeListResponse = TestUtils.listBridges(0, 100).as(BridgeListResponse.class);
        assertThat(bridgeListResponse.getItems().size()).isEqualTo(totalBridges);
        assertThat(bridgeListResponse.getTotal()).isEqualTo(totalBridges);
        assertThat(bridgeListResponse.getPage()).isEqualTo(0);

        bridgeListResponse = TestUtils.listBridges(100, 1).as(BridgeListResponse.class);
        assertThat(bridgeListResponse.getItems().size()).isEqualTo(0);
        assertThat(bridgeListResponse.getTotal()).isEqualTo(totalBridges);
        assertThat(bridgeListResponse.getPage()).isEqualTo(100);

        bridgeListResponse = TestUtils.listBridges(0, 5).as(BridgeListResponse.class);
        assertThat(bridgeListResponse.getItems().size()).isEqualTo(5);
        assertThat(bridgeListResponse.getTotal()).isEqualTo(totalBridges);
        assertThat(bridgeListResponse.getPage()).isEqualTo(0);
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

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME, createKafkaAction())).then().statusCode(202);

        TestUtils.deleteBridge(bridgeResponse.getId()).then().statusCode(400);
    }

    private void assertErrorResponses(ErrorsResponse errorsResponse, Set<Class<? extends RuntimeException>> exceptions) {
        Set<String> expectedErrorCodes = exceptions.stream().map(e -> errorDAO.findByException(e).getCode()).collect(Collectors.toSet());

        assertThat(errorsResponse.getItems().stream().allMatch(x -> x.getHref().contains(V2APIConstants.V2_ERROR_API_BASE_PATH))).isTrue();
        assertThat(errorsResponse.getItems())
                .hasSize(expectedErrorCodes.size())
                .map(ErrorResponse::getCode)
                .allSatisfy(expectedErrorCodes::contains);
    }
}
