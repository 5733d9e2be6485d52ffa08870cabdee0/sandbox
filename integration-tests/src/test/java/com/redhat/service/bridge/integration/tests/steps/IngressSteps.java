package com.redhat.service.bridge.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.integration.tests.resources.IngressResource;

import io.cloudevents.SpecVersion;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
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

        String token = BridgeUtils.retrieveAccessToken();

        IngressResource.postJsonEventResponse(token, StepsContext.endPoint, StepsContext.cloudEventStream, headers)
                .then()
                .statusCode(200);

        StepsContext.cloudEventStream = new ByteArrayInputStream(cloudEvent.getBytes(StandardCharsets.UTF_8));
        IngressResource
                .postJsonEventResponse(token, StepsContext.endPoint + "/ingress/not-the-bridge-name/",
                        StepsContext.cloudEventStream, headers)
                .then()
                .statusCode(404);

        // Plain endpoint for non cloud events payloads
        IngressResource
                .postPlainEventResponse(token, StepsContext.endPoint, "{\"data\": \"test\"}", headers)
                .then()
                .statusCode(200);
    }

    @And("^the Ingress is Undeployed within (\\d+) (?:minute|minutes)$")
    public void ingressUndeployedWithinMinutes(int timeoutMinutes) {
        StepsContext.cloudEventStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        Awaitility.await().atMost(Duration.ofMinutes(timeoutMinutes)).pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> IngressResource
                        .optionsJsonEmptyEventResponse(BridgeUtils.retrieveAccessToken(), StepsContext.endPoint)
                        .then()
                        .statusCode(Matchers.anyOf(Matchers.is(404), Matchers.is(503))));
    }
}
