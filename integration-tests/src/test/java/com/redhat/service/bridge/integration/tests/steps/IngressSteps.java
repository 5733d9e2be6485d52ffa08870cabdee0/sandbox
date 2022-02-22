package com.redhat.service.bridge.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.bridge.integration.tests.common.BridgeUtils;

import io.cloudevents.SpecVersion;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;

public class IngressSteps {
    @Then("^send cloud events to the ingress at the endpoint with access token:$")
    public void testIngressEndpoint(String cloudEvent) {
        StepsContext.cloudEventStream = new ByteArrayInputStream(cloudEvent.getBytes(StandardCharsets.UTF_8));

        Headers headers = new Headers(
                new Header("ce-specversion", SpecVersion.V1.toString()),
                new Header("ce-type", "myType"),
                new Header("ce-id", "myId"),
                new Header("ce-source", "mySource"),
                new Header("ce-subject", "mySubject"));

        BridgeUtils.jsonRequestWithAuth()
                .body(StepsContext.cloudEventStream).contentType(ContentType.JSON)
                .headers(headers)
                .post(StepsContext.endPoint + "/events")
                .then()
                .statusCode(200);

        BridgeUtils.jsonRequestWithAuth()
                .body(StepsContext.cloudEventStream)
                .headers(headers)
                .post(StepsContext.endPoint + "/ingress/events/not-the-bridge-name")
                .then()
                .statusCode(404);

        // Plain endpoint for non cloud events payloads
        BridgeUtils.jsonRequestWithAuth()
                .body("{\"data\": \"test\"}").contentType(ContentType.JSON)
                .headers(headers)
                .post(StepsContext.endPoint + "/events/plain")
                .then()
                .statusCode(200);
    }

    @And("^the Ingress is Undeployed within (\\d+) (?:minute|minutes)$")
    public void ingressUndeployedWithinMinutes(int timeoutMinutes) {
        StepsContext.cloudEventStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        Awaitility.await().atMost(Duration.ofMinutes(timeoutMinutes)).pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> BridgeUtils.jsonRequestWithAuth()
                        .body(StepsContext.cloudEventStream).contentType(ContentType.JSON)
                        .post(StepsContext.endPoint + "/events")
                        .then()
                        .statusCode(Matchers.anyOf(Matchers.is(404), Matchers.is(503))));
    }
}
