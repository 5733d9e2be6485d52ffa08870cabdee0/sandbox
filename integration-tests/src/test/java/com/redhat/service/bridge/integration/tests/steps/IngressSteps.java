package com.redhat.service.bridge.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.IngressResource;

import io.cloudevents.SpecVersion;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.restassured.http.Header;
import io.restassured.http.Headers;

public class IngressSteps {

    private TestContext context;

    public IngressSteps(TestContext context) {
        this.context = context;
    }

    @And("^the Ingress of Bridge \"([^\"]*)\" is deployed within (\\d+) (?:minute|minutes)$")
    public void ingressOfBridgeIsDeployedWithinMinutes(String testBridgeName, int timeoutMinutes) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName);

        Awaitility.await().atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint)
                        .then()
                        .statusCode(200));
    }

    @And("^the Ingress of Bridge \"([^\"]*)\" is undeployed within (\\d+) (?:minute|minutes)$")
    public void ingressOfBridgeIsUndeployedWithinMinutes(String testBridgeName, int timeoutMinutes) {
        System.out.println("hello");
        String endpoint = BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName, true);

        Awaitility.await().atMost(Duration.ofMinutes(timeoutMinutes)).pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint)
                        .then()
                        .statusCode(404));
    }

    @Then("^send a cloud event to the Ingress of the Bridge \"([^\"]*)\":$")
    public void sendCloudEventToIngressOfBridge(String testBridgeName, String cloudEvent) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName);

        InputStream cloudEventStream = new ByteArrayInputStream(cloudEvent.getBytes(StandardCharsets.UTF_8));

        Headers headers = new Headers(
                new Header("ce-specversion", SpecVersion.V1.toString()),
                new Header("ce-type", "myType"),
                new Header("ce-id", "myId"),
                new Header("ce-source", "mySource"),
                new Header("ce-subject", "mySubject"));

        IngressResource.postJsonEventResponse(context.getManagerToken(), endpoint, cloudEventStream, headers)
                .then()
                .statusCode(200);

        // TODO
        // jsonRequestWithAuth()
        //         .body(cloudEventStream)
        //         .headers(headers)
        //         .post(endPoint + "/ingress/events/not-the-bridge-name")
        //         .then()
        //         .statusCode(404);

        // // Plain endpoint for non cloud events payloads
        // jsonRequestWithAuth()
        //         .body("{\"data\": \"test\"}").contentType(ContentType.JSON)
        //         .headers(headers)
        //         .post(endPoint + "/events/plain")
        //         .then()
        //         .statusCode(200);
    }
}
