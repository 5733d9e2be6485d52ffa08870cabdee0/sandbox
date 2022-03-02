package com.redhat.service.bridge.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.IngressResource;

import io.cloudevents.SpecVersion;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.http.Header;
import io.restassured.http.Headers;

public class IngressSteps {

    private TestContext context;

    public IngressSteps(TestContext context) {
        this.context = context;
    }

    @And("^the Ingress of Bridge \"([^\"]*)\" is available within (\\d+) (?:minute|minutes)$")
    public void ingressOfBridgeIsAvailableWithinMinutes(String testBridgeName, int timeoutMinutes) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName);

        Awaitility.await().atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint)
                        .then()
                        .statusCode(200));
    }

    @And("^the Ingress of Bridge \"([^\"]*)\" is not available within (\\d+) (?:minute|minutes)$")
    public void ingressOfBridgeIsNotAvailableWithinMinutes(String testBridgeName, int timeoutMinutes) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName);

        Awaitility.await().atMost(Duration.ofMinutes(timeoutMinutes)).pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint)
                        .then()
                        .statusCode(Matchers.anyOf(Matchers.is(404), Matchers.is(503))));
    }

    @When("^send a cloud event to the Ingress of the Bridge \"([^\"]*)\" with path \"([^\"]*)\":$")
    public void sendCloudEventToIngressOfBridgeWithPath(String testBridgeName, String path, String cloudEvent) {
        sendAndCheckCloudEvent(testBridgeName, cloudEvent, path, 200);
    }

    @When("^send a cloud event to the Ingress of the Bridge \"([^\"]*)\" with path \"([^\"]*)\" is failing with HTTP response code (\\d+):$")
    public void sendCloudEventToIngressOfBridgeWithPathIsFailingWithHTTPResponseCode(String testBridgeName, String path, int responseCode, String cloudEvent) {
        sendAndCheckCloudEvent(testBridgeName, cloudEvent, path, responseCode);
    }

    private void sendAndCheckCloudEvent(String testBridgeName, String cloudEvent, String path, int responseCode) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName);
        endpoint = endpoint + "/" + path;

        InputStream cloudEventStream = new ByteArrayInputStream(cloudEvent.getBytes(StandardCharsets.UTF_8));

        Headers headers = new Headers(
                new Header("ce-specversion", SpecVersion.V1.toString()),
                new Header("ce-type", "myType"),
                new Header("ce-id", "myId"),
                new Header("ce-source", "mySource"),
                new Header("ce-subject", "mySubject"));

        String token = context.getManagerToken();
        IngressResource.postCloudEventResponse(token, endpoint, cloudEventStream, headers)
                .then()
                .statusCode(responseCode);
    }
}
