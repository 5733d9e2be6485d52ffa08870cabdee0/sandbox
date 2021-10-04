package com.redhat.service.bridge.runner.it;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTestResource(KafkaResource.class)
@QuarkusTestResource(PostgresResource.class)
public class End2EndTestIT {

    private static final String bridgeName = "notificationBridge";
    private static final String processorName = "myProcessor";
    private static final String topicName = "myKafkaTopic";
    private static final Set<BaseFilter> filters = Collections.singleton(new StringEquals("key", "createdEvent"));
    private static final String transformationTemplate = "{\"v1\": \"{data.k1}\"}";

    private static String bridgeId;
    private static String processorId;

    private static ProcessorRequest processorRequest;

    @ConfigProperty(name = "event-bridge.manager.url")
    String managerUrl;

    @Inject
    AdminClient adminClient;

    @BeforeAll
    public static void beforeAll() {
        BaseAction action = new BaseAction();
        action.setName("myKafkaAction");
        action.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, topicName);
        action.setParameters(params);

        processorRequest = new ProcessorRequest(processorName, filters, transformationTemplate, action);
    }

    @Order(1)
    @Test
    public void getEmptyBridges() {
        BridgeListResponse response = jsonRequest()
                .get(managerUrl + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(200)
                .extract()
                .as(BridgeListResponse.class);

        assertThat(response.getKind()).isEqualTo("BridgeList");
        assertThat(response.getSize()).isZero();
        assertThat(response.getPage()).isZero();
        assertThat(response.getTotal()).isZero();
    }

    @Order(2)
    @Test
    public void createBridge() {
        BridgeResponse response = jsonRequest()
                .body(new BridgeRequest(bridgeName))
                .post(managerUrl + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .as(BridgeResponse.class);

        assertThat(response.getName()).isEqualTo(bridgeName);
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(response.getEndpoint()).isNull();
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getSubmittedAt()).isNotNull();

        bridgeId = response.getId();
    }

    @Order(3)
    @Test
    public void testBridgeIsDeployed() {
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> jsonRequest()
                                .get(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId)
                                .then()
                                .body("status", Matchers.equalTo("AVAILABLE"))
                                .body("endpoint", Matchers.equalTo("/ingress/events/" + bridgeId)));
    }

    @Order(4)
    @Test
    public void testAddProcessor() throws Exception {

        /*
         * Ensure that the requested Kafka Topic for the Action exists
         */
        NewTopic nt = new NewTopic(topicName, 1, (short) 1);
        adminClient.createTopics(Collections.singleton(nt)).all().get(10L, TimeUnit.SECONDS);

        ProcessorResponse response = jsonRequest()
                .body(processorRequest)
                .post(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors")
                .then()
                .statusCode(201)
                .extract()
                .as(ProcessorResponse.class);

        processorId = response.getId();

        assertThat(response.getName()).isEqualTo(processorName);
        assertThat(response.getKind()).isEqualTo("Processor");
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getBridge()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(response.getFilters().size()).isEqualTo(1);

        BaseAction action = response.getAction();
        assertThat(action.getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(action.getParameters().get(KafkaTopicAction.TOPIC_PARAM)).isEqualTo(topicName);
    }

    @Order(5)
    @Test
    public void testProcessorIsDeployed() {
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> jsonRequest()
                                .get(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId)
                                .then()
                                .body("status", Matchers.equalTo("AVAILABLE")));
    }

    @Order(6)
    @Test
    public void testIngressEndpoint() throws JsonProcessingException {
        jsonRequest()
                .body(buildTestCloudEvent())
                .post(managerUrl + "/ingress/events/not-the-bridge-name")
                .then()
                .statusCode(500);

        jsonRequest()
                .body(buildTestCloudEvent())
                .post(managerUrl + "/ingress/events/" + bridgeId)
                .then()
                .statusCode(200);
    }

    @Order(7)
    @Test
    public void testDeleteProcessors() {
        jsonRequest()
                .delete(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId)
                .then()
                .statusCode(202);

        // Processor instance is undeployed and deleted
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> jsonRequest()
                                .get(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId)
                                .then()
                                .statusCode(404));
    }

    @Order(8)
    @Test
    public void testDeleteBridge() throws JsonProcessingException {
        jsonRequest()
                .delete(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId)
                .then()
                .statusCode(202);

        // Bridge instance is undeployed and deleted
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> jsonRequest()
                                .get(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId)
                                .then()
                                .statusCode(404));

        // Ingress application is undeployed
        jsonRequest()
                .body(buildTestCloudEvent())
                .post(managerUrl + "/ingress/events/" + bridgeName)
                .then()
                .statusCode(500);
    }

    // TODO: Add processors integration tests when CRUD api will be implemented.

    private String buildTestCloudEvent() throws JsonProcessingException {
        String jsonString = "{\"k1\":\"v1\",\"k2\":\"v2\"}";
        return CloudEventUtils.encode(
                CloudEventUtils.build("myId", "myTopic", URI.create("mySource"), "subject", new ObjectMapper().readTree(jsonString)));
    }

    private RequestSpecification jsonRequest() {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when();
    }
}
