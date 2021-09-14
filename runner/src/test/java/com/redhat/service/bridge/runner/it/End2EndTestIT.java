package com.redhat.service.bridge.runner.it;

import java.net.URI;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTestResource(KafkaResource.class)
@QuarkusTestResource(PostgresResource.class)
public class End2EndTestIT {
    private static final String bridgeName = "notificationBridge";

    private static String bridgeId;

    @ConfigProperty(name = "event-bridge.manager.url")
    String managerUrl;

    @Order(1)
    @Test
    public void getEmptyBridges() {
        BridgeListResponse response = jsonRequest()
                .get(managerUrl + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(200)
                .extract()
                .as(BridgeListResponse.class);

        Assertions.assertEquals("BridgeList", response.getKind());
        Assertions.assertEquals(0, response.getSize());
        Assertions.assertEquals(0, response.getPage());
        Assertions.assertEquals(0, response.getTotal());
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

        Assertions.assertEquals(bridgeName, response.getName());
        Assertions.assertEquals(BridgeStatus.REQUESTED, response.getStatus());
        Assertions.assertNull(response.getEndpoint());
        Assertions.assertNull(response.getPublishedAt());
        Assertions.assertNotNull(response.getHref());
        Assertions.assertNotNull(response.getSubmittedAt());

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
                                .body(new BridgeRequest(bridgeName))
                                .get(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId)
                                .then()
                                .body("status", Matchers.equalTo("AVAILABLE"))
                                .body("endpoint", Matchers.equalTo("/ingress/events/" + bridgeId)));
    }

    @Order(4)
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

    @Order(5)
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
