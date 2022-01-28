package com.redhat.service.bridge.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

import io.cloudevents.SpecVersion;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;

import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.addBridge;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.createProcessor;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.deleteBridge;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.deleteProcessor;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.getBridgeDetails;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.getBridgeList;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.getProcessor;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.managerUrl;
import static com.redhat.service.bridge.integration.tests.common.BridgeUtils.jsonRequestWithAuth;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class End2EndTestITStep {

    private static final String TOPIC_NAME = "myKafkaTopic";
    private static final String PROCESSOR_NAME = "myProcessor";

    private static String bridgeId;
    private static String processorId;
    private static String endPoint;
    private static String ingressMetrics;
    private static InputStream cloudEventStream;

    @Given("get list of Bridge instances returns HTTP response code 401")
    public void authenticationIsEnabled() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(managerUrl + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(401);
    }

    @Given("^get list of Bridge instances with access token doesn't contain Bridge \"([^\"]*)\"$")
    public void getEmptyBridges(String bridgeName) {
        BridgeListResponse response = getBridgeList();
        assertThat(response.getKind()).isEqualTo("BridgeList");
        if (response.getItems().size() > 0) {
            for (int i = 0; i < response.getItems().size(); i++) {
                assertThat(response.getItems().get(i).getName()).isNotEqualTo(bridgeName);
            }
        }
    }

    @When("^create a Bridge with name \"([^\"]*)\" with access token$")
    public void createBridge(String bridgeName) {
        BridgeResponse response = addBridge(bridgeName);
        assertThat(response.getName()).isEqualTo(bridgeName);
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(response.getEndpoint()).isNull();
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getSubmittedAt()).isNotNull();

        bridgeId = response.getId();
    }

    @Given("^get list of Bridge instances with access token contains Bridge \"([^\"]*)\"")
    public void testBridgeExists(String bridgeName) {
        BridgeListResponse response = getBridgeList();
        List<String> bridgeNames = new ArrayList<>();
        if (response.getItems().size() > 0) {
            for (int i = 0; i < response.getItems().size(); i++) {
                bridgeNames.add(response.getItems().get(i).getName());
            }
        }
        assertThat(bridgeNames.contains(bridgeName)).isTrue();
    }

    @Then("^get Bridge with access token exists in status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void bridgeDeployedWithinMinutes(String status, int timeoutMinutes) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> getBridgeDetails(bridgeId)
                                .then()
                                .body("status", Matchers.equalTo(status))
                                .body("endpoint", Matchers.containsString(bridgeId)));

        // store bridge endpoint details
        endPoint = getBridgeDetails(bridgeId).then().extract().response().as(BridgeResponse.class).getEndpoint();
    }

    @When("^add Processor to the Bridge with access token:$")
    public void addProcessor(String processorRequestJson) {
        InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8));
        ProcessorResponse response = createProcessor(bridgeId, resourceStream);

        processorId = response.getId();

        assertThat(response.getName()).isEqualTo(PROCESSOR_NAME);
        assertThat(response.getKind()).isEqualTo("Processor");
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(response.getFilters().size()).isEqualTo(1);

        BaseAction action = response.getAction();
        assertThat(action.getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(action.getParameters()).containsEntry(KafkaTopicAction.TOPIC_PARAM, TOPIC_NAME);
    }

    @Then("add invalid Processor to the Bridge with access token returns HTTP response code 400:$")
    public void addWrongFilterProcessor(String processorRequestJson) {
        InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8));
        jsonRequestWithAuth()
                .body(resourceStream)
                .post(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors")
                .then()
                .statusCode(400);
    }

    @Then("^get Processor with access token exists in status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void processorExistsWithinMinutes(String status, int timeoutMinutes) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> getProcessor(bridgeId, processorId)
                                .then()
                                .body("status", Matchers.equalTo(status)));
    }

    @Then("^send cloud events to the ingress at the endpoint with access token:$")
    public void testIngressEndpoint(String cloudEvent) {
        cloudEventStream = new ByteArrayInputStream(cloudEvent.getBytes(StandardCharsets.UTF_8));

        Headers headers = new Headers(
                new Header("ce-specversion", SpecVersion.V1.toString()),
                new Header("ce-type", "myType"),
                new Header("ce-id", "myId"),
                new Header("ce-source", "mySource"),
                new Header("ce-subject", "mySubject"));

        jsonRequestWithAuth()
                .body(cloudEventStream).contentType(ContentType.JSON)
                .headers(headers)
                .post(endPoint + "/events")
                .then()
                .statusCode(200);

        jsonRequestWithAuth()
                .body(cloudEventStream)
                .headers(headers)
                .post(endPoint + "/ingress/events/not-the-bridge-name")
                .then()
                .statusCode(404);

        // Plain endpoint for non cloud events payloads
        jsonRequestWithAuth()
                .body("{\"data\": \"test\"}").contentType(ContentType.JSON)
                .headers(headers)
                .post(endPoint + "/events/plain")
                .then()
                .statusCode(200);

        // store ingress metrics
        ingressMetrics = jsonRequestWithAuth()
                .delete(endPoint + "/q/metrics")
                .then()
                .extract()
                .body()
                .asString();
    }

    @When("the Processor is deleted")
    public void testDeleteProcessor() {
        deleteProcessor(bridgeId, processorId);
    }

    @Then("^the Processor doesn't exists within (\\d+) (?:minute|minutes)$")
    public void processorDoesNotExistsWithinMinutes(int timeoutMinutes) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> getProcessor(bridgeId, processorId)
                                .then()
                                .statusCode(404));
    }

    @When("delete a Bridge")
    public void testDeleteBridge() {
        deleteBridge(bridgeId);
    }

    @Then("^the Bridge doesn't exists within (\\d+) (?:minute|minutes)$")
    public void bridgeDoesNotExistWithinMinutes(int timeoutMinutes) {

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> getBridgeDetails(bridgeId)
                                .then()
                                .statusCode(404));
    }

    @And("^the Ingress is Undeployed within (\\d+) (?:minute|minutes)$")
    public void ingressUndeployedWithinMinutes(int timeoutMinutes) {
        Awaitility.await().atMost(Duration.ofMinutes(timeoutMinutes)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> jsonRequestWithAuth()
                .body(cloudEventStream).contentType(ContentType.JSON)
                .post(endPoint + "/events")
                .then()
                .statusCode(404));
    }

    @Given("the Metrics info is exists")
    public void testMetrics() {
        String metrics = jsonRequestWithAuth()
                .delete(managerUrl + "/q/metrics")
                .then()
                .extract()
                .body()
                .asString();

        assertThat(metrics).contains("manager_bridge_status_change_total{status=\"AVAILABLE\",} 1.0");
        assertThat(metrics).contains("manager_bridge_status_change_total{status=\"PROVISIONING\",} 1.0");
        assertThat(metrics).contains("manager_bridge_status_change_total{status=\"DELETED\",} 1.0");
        assertThat(metrics).contains("manager_processor_status_change_total{status=\"AVAILABLE\",} 1.0");
        assertThat(metrics).contains("manager_processor_status_change_total{status=\"PROVISIONING\",} 1.0");
        assertThat(metrics).contains("manager_processor_status_change_total{status=\"DELETED\",} 1.0");

        assertThat(ingressMetrics).contains("http_server_requests_seconds_count{method=\"POST\",outcome=\"SUCCESS\",status=\"200\",uri=\"/events\",} 1.0");
        assertThat(ingressMetrics).contains("http_server_requests_seconds_count{method=\"POST\",outcome=\"SUCCESS\",status=\"200\",uri=\"/events/plain\",} 1.0");
    }

}
