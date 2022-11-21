package com.redhat.service.smartevents.manager.v2.api.user;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.api.APIConstants;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createReadyConditions;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ProcessorAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    BridgeDAO bridgeDAO;

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
    public void testAuthentication() {
        TestUtils.getProcessor(TestConstants.DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    @Disabled("BridgeService.getReadyBridge(..) needs to be implemented.")
    public void addProcessorToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(202);

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName()).isEqualTo("myProcessor");

        ProcessorResponse retrieved = TestUtils.getProcessor(bridgeResponse.getId(), processorResponse.getId()).as(ProcessorResponse.class);
        assertThat(retrieved.getName()).isEqualTo("myProcessor");
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addProcessorWithWrongDefinitionToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        String requestBody = "{" +
                "\"name\": \"processorInvalid\"," +
                "\"flange\": {" +
                "  }" +
                "}";

        Response response = TestUtils.addProcessorToBridgeWithRequestBody(bridgeResponse.getId(), requestBody);
        assertThat(response.getStatusCode()).isEqualTo(400);

        ErrorsResponse errors = response.as(ErrorsResponse.class);
        assertThat(errors.getItems()).hasSize(1);

        ErrorResponse error = errors.getItems().get(0);
        assertThat(error.getId()).isEqualTo("21");
        assertThat(error.getCode()).isEqualTo("OPENBRIDGE-21");
        assertThat(error.getReason()).contains("Processor flows cannot be null");
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    @Disabled("BridgeService.getReadyBridge(..) needs to be implemented.")
    public void addProcessorToBridge_bridgeDoesNotExist() {
        Response response = TestUtils.addProcessorToBridge("foo", new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    @Disabled("BridgeService.getReadyBridge(..) needs to be implemented.")
    public void addProcessorToBridge_bridgeNotInReadyStatus() {
        BridgeResponse bridgeResponse = createBridge();
        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    public void addProcessorToBridgeNoAuthentication() {
        Response response = TestUtils.addProcessorToBridge(TestConstants.DEFAULT_BRIDGE_NAME, new ProcessorRequest());
        assertThat(response.getStatusCode()).isEqualTo(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    @Disabled("BridgeService.getReadyBridge(..) needs to be implemented.")
    public void createProcessorOrganisationWithNoQuota() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn("organisation-with-no-quota");
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"))
                .then().statusCode(402).body("kind", Matchers.equalTo("Errors"));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    @Disabled("BridgeService.getReadyBridge(..) needs to be implemented.")
    public void basicMetricsForBridgeAndProcessors() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2")).as(ProcessorResponse.class);

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
                .contains("http_server_requests_seconds_count{method=\"POST\",outcome=\"SUCCESS\",status=\"202\",uri=\"/api/smartevents_mgmt/v2/bridges\",}")
                .contains("http_server_requests_seconds_sum{method=\"POST\",outcome=\"SUCCESS\",status=\"202\",uri=\"/api/smartevents_mgmt/v2/bridges\",}")
                .contains("http_server_requests_seconds_count{method=\"POST\",outcome=\"SUCCESS\",status=\"202\",uri=\"/api/smartevents_mgmt/v2/bridges/" + bridgeResponse.getId() + "/processors\",}")
                .contains("http_server_requests_seconds_sum{method=\"POST\",outcome=\"SUCCESS\",status=\"202\",uri=\"/api/smartevents_mgmt/v2/bridges/" + bridgeResponse.getId() + "/processors\",}");
    }

    private BridgeResponse createBridge() {
        BridgeRequest r = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CLOUD_PROVIDER, TestConstants.DEFAULT_REGION);
        return TestUtils.createBridge(r).as(BridgeResponse.class);
    }

    protected BridgeResponse createAndDeployBridge() {
        BridgeResponse bridgeResponse = createBridge();
        setBridgeStatus(bridgeResponse.getId(), createReadyConditions());
        return bridgeResponse;
    }

    @Transactional
    protected void setBridgeStatus(String bridgeId, List<Condition> conditions) {
        Bridge bridge = bridgeDAO.findById(bridgeId);
        bridge.setConditions(conditions);
    }

}
