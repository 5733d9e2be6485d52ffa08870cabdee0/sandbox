package com.redhat.service.bridge.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

import io.cloudevents.SpecVersion;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.json.JsonObject;

import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.addBridge;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.createProcessor;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.deleteBridge;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.deleteProcessor;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.getBridgeDetails;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.getBridgeList;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.getProcessor;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.listProcessors;
import static com.redhat.service.bridge.integration.tests.common.BridgeCommon.managerUrl;
import static com.redhat.service.bridge.integration.tests.common.BridgeUtils.jsonRequestWithAuth;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class End2EndTestITStep {

    private static String randomBridgeName;
    private static String bridgeId;
    private static String processorId;
    private static String endPoint;
    private static InputStream cloudEventStream;

    @Given("get list of Bridge instances returns HTTP response code (\\d+)$")
    public void authenticationIsEnabled(int responseCode) {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(managerUrl + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(responseCode);
    }

    @Given("get list of Bridge instances with access token doesn't contain randomly generated Bridge")
    public void generateRandomBridgeName() {
        End2EndTestITStep.randomBridgeName = "bridge-" + UUID.randomUUID().toString().substring(0, 4);
        BridgeListResponse response = getBridgeList();
        assertThat(response.getKind()).isEqualTo("BridgeList");
        assertThat(response.getItems()).noneMatch(b -> b.getName().equals(End2EndTestITStep.randomBridgeName));
    }

    @When("create a Bridge with randomly generated name with access token")
    public void createRandomBridge() {
        BridgeResponse response = addBridge(End2EndTestITStep.randomBridgeName);
        assertThat(response.getName()).isEqualTo(End2EndTestITStep.randomBridgeName);
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(response.getEndpoint()).isNull();
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getSubmittedAt()).isNotNull();

        bridgeId = response.getId();
    }

    @Then("get list of Bridge instances with access token contains Bridge with randomly generated name")
    public void testRandomBridgeExists() {
        BridgeListResponse response = getBridgeList();
        assertThat(response.getItems()).anyMatch(b -> b.getName().equals(End2EndTestITStep.randomBridgeName));
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
        // If an endpoint contains localhost without port then default port has to be defined, otherwise rest-assured will use port 8080
        if (endPoint.matches("http://localhost/.*")) {
            endPoint = endPoint.replace("http://localhost/", "http://localhost:80/");
        }
    }

    @When("^add Processor to the Bridge with access token:$")
    public void addProcessor(String processorRequestJson) {

        JsonObject json = new JsonObject(processorRequestJson);
        String processorName = json.getString("name");
        String topic = json.getJsonObject("action").getJsonObject("parameters").getString("topic");
        int filtersSize = json.getJsonArray("filters").size();

        InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8));
        ProcessorResponse response = createProcessor(bridgeId, resourceStream);

        processorId = response.getId();

        assertThat(response.getName()).isEqualTo(processorName);
        assertThat(response.getKind()).isEqualTo("Processor");
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(response.getFilters().size()).isEqualTo(filtersSize);

        BaseAction action = response.getAction();
        assertThat(action.getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(action.getParameters()).containsEntry(KafkaTopicAction.TOPIC_PARAM, topic);
    }

    @Then("add invalid Processor to the Bridge with access token returns HTTP response code (\\d+):$")
    public void addWrongFilterProcessor(int responseCode, String processorRequestJson) {
        InputStream resourceStream = new ByteArrayInputStream(processorRequestJson.getBytes(StandardCharsets.UTF_8));
        jsonRequestWithAuth()
                .body(resourceStream)
                .post(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors")
                .then()
                .statusCode(responseCode);
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
        cloudEventStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        Awaitility.await().atMost(Duration.ofMinutes(timeoutMinutes)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> jsonRequestWithAuth()
                .body(cloudEventStream).contentType(ContentType.JSON)
                .post(endPoint + "/events")
                .then()
                .statusCode(Matchers.anyOf(Matchers.is(404), Matchers.is(503))));
    }

    @Given("^the Manager Metric \'([^\']*)\' count is at least (\\d+)$")
    public void managerMetricCount(String metricName, int minimalValue) {
        testMetricAndCount(managerUrl + "/q/metrics", metricName, minimalValue);
    }

    @Given("^the Ingress Metric \'([^\']*)\' count is at least (\\d+)$")
    public void ingressMetricCount(String metricName, int minimalValue) {
        testMetricAndCount(endPoint + "/q/metrics", metricName, minimalValue);
    }

    private void testMetricAndCount(String metricEndpoint, String metricName, int minimalValue) {
        String metrics = jsonRequestWithAuth()
                .get(metricEndpoint)
                .then()
                .extract()
                .body()
                .asString();

        assertThat(metrics).contains(metricName);
        metrics.lines()
                .filter(l -> l.contains(metricName))
                .map(m -> m.replace(metricName + " ", ""))
                .mapToDouble(m -> Double.parseDouble(m))
                .forEach(d -> assertThat(d).as("Checking %s value", metricName).isGreaterThanOrEqualTo(minimalValue));
    }

    @After
    public void cleanUp() {
        if (getBridgeList().getItems().stream().anyMatch(b -> b.getId().equals(bridgeId))) {
            if (listProcessors(bridgeId).getSize() > 0) {
                listProcessors(bridgeId).getItems().stream().forEach(p -> deleteProcessor(bridgeId, p.getId()));
                Awaitility.await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(5)).until(() -> listProcessors(bridgeId).getSize() == 0);
            }
            deleteBridge(bridgeId);
        }
    }

}
