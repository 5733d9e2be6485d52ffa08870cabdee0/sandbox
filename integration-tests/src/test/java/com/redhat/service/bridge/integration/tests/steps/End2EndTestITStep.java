package com.redhat.service.bridge.integration.tests.steps;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.infra.models.filters.ValuesIn;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.integration.tests.common.AbstractBridge;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

import io.cloudevents.SpecVersion;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.filter.log.ResponseLoggingFilter;
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
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class End2EndTestITStep extends AbstractBridge {

    private static final String TOPIC_NAME = "myKafkaTopic";
    private static final Set<BaseFilter> FILTERS = Collections.singleton(new StringEquals("source", "StorageService"));
    private static final String TRANSFORMATION_TEMPLATE = "{\"v1\": \"{data.k1}\"}";
    private static final String PROCESSOR_NAME = "myProcessor";
    private static String BRIDGE_NAME = "notificationBridge";

    private static String bridgeId;
    private static String processorId;
    private static String endPoint;
    private static String ingressMetrics;
    private static ProcessorRequest processorRequest;

    @Given("Manager url is not accessible")
    public void authenticationIsEnabled() {
        given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get(managerUrl + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(401);
    }

    @Given("Bridge list is empty")
    public void getEmptyBridges() {

        BridgeListResponse response = getBridgeList();
        assertThat(response.getKind()).isEqualTo("BridgeList");
        assertThat(response.getSize()).isZero();
        assertThat(response.getPage()).isZero();
        assertThat(response.getTotal()).isZero();
    }

    @When("Create a Bridge")
    public void createBridge() {
        BridgeResponse response = addBridge(BRIDGE_NAME);
        assertThat(response.getName()).isEqualTo(BRIDGE_NAME);
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(response.getEndpoint()).isNull();
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getSubmittedAt()).isNotNull();

        bridgeId = response.getId();
    }

    @Then("^Bridge is exists in status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
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

    @When("Add processor to the bridge")
    public void addProcessor() {

        processorRequest = getProcessorRequest(FILTERS);

        ProcessorResponse response = createProcessor(bridgeId, processorRequest);

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

    @Then("Add wrong filter processor")
    public void addWrongFilterProcessor() {
        jsonRequestWithAuth()
                .body(getProcessorRequest(Collections.singleton(new ValuesIn())))
                .post(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors")
                .then()
                .statusCode(400);
    }

    @Then("^Processor is exists in status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void processorExistsWithinMinutes(String status, int timeoutMinutes) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> getProcessor(bridgeId, processorId)
                                .then()
                                .body("status", Matchers.equalTo(status)));
    }

    @Then("Ingress endpoint is accessible")
    public void testIngressEndpoint() throws JsonProcessingException {
        Headers headers = new Headers(
                new Header("ce-specversion", SpecVersion.V1.toString()),
                new Header("ce-type", "myType"),
                new Header("ce-id", "myId"),
                new Header("ce-source", "mySource"),
                new Header("ce-subject", "mySubject"));

        jsonRequestWithAuth()
                .body(buildTestCloudEvent()).contentType(ContentType.JSON)
                .headers(headers)
                .post(endPoint + "/events")
                .then()
                .statusCode(200);

        jsonRequestWithAuth()
                .body(buildTestCloudEvent())
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

    @When("Processor is deleted")
    public void testDeleteProcessor() {
        deleteProcessor(bridgeId, processorId);
    }

    @Then("^Processor doesn't exists within (\\d+) (?:minute|minutes)$")
    public void processorDoesNotExistsWithinMinutes(int timeoutMinutes) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> getProcessor(bridgeId, processorId)
                                .then()
                                .statusCode(404));
    }

    @When("Delete a Bridge")
    public void testDeleteBridge() {
        deleteBridge(bridgeId);
    }

    @Then("^Bridge doesn't exists within (\\d+) (?:minute|minutes)$")
    public void bridgeDoesNotExistWithinMinutes(int timeoutMinutes) {

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> getBridgeDetails(bridgeId)
                                .then()
                                .statusCode(404));
    }

    @And("^Ingress is Undeployed within (\\d+) (?:minute|minutes)$")
    public void ingressUndeployedWithinMinutes(int timeoutMinutes) {

        Awaitility.await().atMost(Duration.ofMinutes(timeoutMinutes)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> jsonRequestWithAuth()
                .body(buildTestCloudEvent()).contentType(ContentType.JSON)
                .post(endPoint + "/events")
                .then()
                .statusCode(404));
    }

    @Given("Metrics info is exists")
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

    private static ProcessorRequest getProcessorRequest(Set<BaseFilter> filters) {
        BaseAction action = new BaseAction();
        action.setName("myKafkaAction");
        action.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TOPIC_NAME);
        action.setParameters(params);
        return new ProcessorRequest(PROCESSOR_NAME, FILTERS, TRANSFORMATION_TEMPLATE, action);
    }

    private String buildTestCloudEvent() throws JsonProcessingException {

        String jsonString = "{\"k1\":\"v1\",\"k2\":\"v2\"}";
        return CloudEventUtils.encode(
                CloudEventUtils.build("myId", SpecVersion.V1, URI.create("mySource"), "subject", new ObjectMapper().readTree(jsonString)));
    }
}
