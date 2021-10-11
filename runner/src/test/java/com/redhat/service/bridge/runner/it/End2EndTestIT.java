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
import org.keycloak.representations.AccessTokenResponse;

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
@QuarkusTestResource(KeycloakResource.class)
public class End2EndTestIT {

    private static final String USER_NAME = "kermit";
    private static final String PASSWORD = "thefrog";
    private static final String BRIDGE_NAME = "notificationBridge";
    private static final String PROCESSOR_NAME = "myProcessor";
    private static final String TOPIC_NAME = "myKafkaTopic";
    private static final Set<BaseFilter> FILTERS = Collections.singleton(new StringEquals("key", "createdEvent"));
    private static final String TRANSFORMATION_TEMPLATE = "{\"v1\": \"{data.k1}\"}";

    private static String bridgeId;
    private static String processorId;
    private static String token;

    private static ProcessorRequest processorRequest;

    @ConfigProperty(name = "event-bridge.manager.url")
    String managerUrl;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String keycloakURL;

    @Inject
    AdminClient adminClient;

    @BeforeAll
    public static void beforeAll() {
        BaseAction action = new BaseAction();
        action.setName("myKafkaAction");
        action.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TOPIC_NAME);
        action.setParameters(params);

        processorRequest = new ProcessorRequest(PROCESSOR_NAME, FILTERS, TRANSFORMATION_TEMPLATE, action);
    }

    @Test
    public void authenticationIsEnabled() {
        given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get(managerUrl + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(401);
    }

    @Order(1)
    @Test
    public void getEmptyBridges() {
        BridgeListResponse response = jsonRequestWithAuth()
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
        BridgeResponse response = jsonRequestWithAuth()
                .body(new BridgeRequest(BRIDGE_NAME))
                .post(managerUrl + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .as(BridgeResponse.class);

        assertThat(response.getName()).isEqualTo(BRIDGE_NAME);
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
                        () -> jsonRequestWithAuth()
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
        NewTopic nt = new NewTopic(TOPIC_NAME, 1, (short) 1);
        adminClient.createTopics(Collections.singleton(nt)).all().get(10L, TimeUnit.SECONDS);

        ProcessorResponse response = jsonRequestWithAuth()
                .body(processorRequest)
                .post(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors")
                .then()
                .statusCode(201)
                .extract()
                .as(ProcessorResponse.class);

        processorId = response.getId();

        assertThat(response.getName()).isEqualTo(PROCESSOR_NAME);
        assertThat(response.getKind()).isEqualTo("Processor");
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getBridge()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(response.getFilters().size()).isEqualTo(1);

        BaseAction action = response.getAction();
        assertThat(action.getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(action.getParameters()).containsEntry(KafkaTopicAction.TOPIC_PARAM, TOPIC_NAME);
    }

    @Order(5)
    @Test
    public void testProcessorIsDeployed() {
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> jsonRequestWithAuth()
                                .get(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId)
                                .then()
                                .body("status", Matchers.equalTo("AVAILABLE")));
    }

    @Order(6)
    @Test
    public void testIngressEndpoint() throws JsonProcessingException {
        jsonRequestWithAuth()
                .body(buildTestCloudEvent())
                .post(managerUrl + "/ingress/events/not-the-bridge-name")
                .then()
                .statusCode(500);

        jsonRequestWithAuth()
                .body(buildTestCloudEvent())
                .post(managerUrl + "/ingress/events/" + bridgeId)
                .then()
                .statusCode(200);
    }

    @Order(7)
    @Test
    public void testDeleteProcessors() {
        jsonRequestWithAuth()
                .delete(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId)
                .then()
                .statusCode(202);

        // Processor instance is undeployed and deleted
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> jsonRequestWithAuth()
                                .get(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId)
                                .then()
                                .statusCode(404));
    }

    @Order(8)
    @Test
    public void testDeleteBridge() throws JsonProcessingException {
        jsonRequestWithAuth()
                .delete(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId)
                .then()
                .statusCode(202);

        // Bridge instance is undeployed and deleted
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> jsonRequestWithAuth()
                                .get(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId)
                                .then()
                                .statusCode(404));

        // Ingress application is undeployed
        jsonRequestWithAuth()
                .body(buildTestCloudEvent())
                .post(managerUrl + "/ingress/events/" + BRIDGE_NAME)
                .then()
                .statusCode(500);
    }

    @Order(9)
    @Test
    public void testMetrics() {
        String metrics = jsonRequestWithAuth()
                .delete(managerUrl + "/q/metrics")
                .then()
                .extract()
                .body()
                .asString();

        assertThat(metrics).contains("executor_entire_processing_evaluation_seconds_count");
        assertThat(metrics).contains("executor_filter_evaluation_seconds_count");
        assertThat(metrics).contains("executor_transformation_evaluation_seconds_count");
        assertThat(metrics).contains("executor_action_evaluation_seconds_count");

        assertThat(metrics).contains("manager_bridge_status_change_total{status=\"AVAILABLE\",} 1.0");
        assertThat(metrics).contains("manager_bridge_status_change_total{status=\"PROVISIONING\",} 1.0");
        assertThat(metrics).contains("manager_bridge_status_change_total{status=\"DELETED\",} 1.0");
        assertThat(metrics).contains("manager_processor_status_change_total{status=\"AVAILABLE\",} 1.0");
        assertThat(metrics).contains("manager_processor_status_change_total{status=\"PROVISIONING\",} 1.0");
        assertThat(metrics).contains("manager_processor_status_change_total{status=\"DELETED\",} 1.0");

        assertThat(metrics).contains("http_server_requests_seconds_count{method=\"POST\",outcome=\"SUCCESS\",status=\"200\",uri=\"/ingress/events/{name}\",} 1.0");
    }

    private String getAccessToken(String userName, String password) {
        return given().param("grant_type", "password")
                .param("username", userName)
                .param("password", password)
                .param("client_id", KeycloakResource.CLIENT_ID)
                .param("client_secret", KeycloakResource.CLIENT_SECRET)
                .when()
                .post(keycloakURL + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class)
                .getToken();
    }

    // TODO: Add processors integration tests when CRUD api will be implemented.

    private String buildTestCloudEvent() throws JsonProcessingException {
        String jsonString = "{\"k1\":\"v1\",\"k2\":\"v2\"}";
        return CloudEventUtils.encode(
                CloudEventUtils.build("myId", "myTopic", URI.create("mySource"), "subject", new ObjectMapper().readTree(jsonString)));
    }

    private RequestSpecification jsonRequestWithAuth() {
        if (token == null) {
            token = getAccessToken(USER_NAME, PASSWORD);
        }

        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .auth()
                .oauth2(token);
    }
}
