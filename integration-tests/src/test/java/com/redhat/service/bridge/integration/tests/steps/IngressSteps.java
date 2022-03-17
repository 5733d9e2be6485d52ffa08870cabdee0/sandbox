package com.redhat.service.bridge.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.integration.tests.common.AwaitilityOnTimeOutHandler;
import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.IngressResource;

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

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(
                        () -> IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint)))
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint)
                        .then()
                        .statusCode(200));
    }

    @And("^the Ingress of Bridge \"([^\"]*)\" is not available within (\\d+) (?:minute|minutes)$")
    public void ingressOfBridgeIsNotAvailableWithinMinutes(String testBridgeName, int timeoutMinutes) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName);

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(
                        () -> IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint)))
                .atMost(Duration.ofMinutes(timeoutMinutes)).pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint)
                        .then()
                        .statusCode(Matchers.anyOf(Matchers.is(404), Matchers.is(503))));
    }

    @When("^send a cloud event to the Ingress of the Bridge \"([^\"]*)\" with path \"([^\"]*)\":$")
    public void sendCloudEventToIngressOfBridgeWithPath(String testBridgeName, String path, String cloudEvent) {
        sendAndCheckCloudEvent(testBridgeName, cloudEvent, path, 200);
    }

    @When("^send a cloud event to the Ingress of the Bridge \"([^\"]*)\" with path \"([^\"]*)\" and headers (\"[^\"]+\":\"[^\"]+\"(?:,\"[^\"]+\":\"[^\"]+\")*):$")
    public void sendCloudEventToIngressOfBridgeWithPathAndDefaultHeaders(String testBridgeName, String path, String headers, String cloudEvent) {
        sendAndCheckCloudEventWithHeaders(testBridgeName, cloudEvent, path, parseHeaders(headers), 200);
    }

    @When("^send a cloud event to the Ingress of the Bridge \"([^\"]*)\" with path \"([^\"]*)\" is failing with HTTP response code (\\d+):$")
    public void sendCloudEventToIngressOfBridgeWithPathIsFailingWithHTTPResponseCode(String testBridgeName, String path,
                                                                                     int responseCode, String cloudEvent) {
        sendAndCheckCloudEvent(testBridgeName, cloudEvent, path, responseCode);
    }

    @When("^send a cloud event to the Ingress of the Bridge \"([^\"]*)\" with path \"([^\"]*)\" and headers (\"[^\"]+\":\"[^\"]+\"(?:,\"[^\"]+\":\"[^\"]+\")*) is failing with HTTP response code (\\d+):$")
    public void sendCloudEventToIngressOfBridgeWithPathAndDefaultHeadersIsFailingWithHTTPResponseCode(String testBridgeName,
                                                                                                      String path, String headers, int responseCode, String cloudEvent) {
        sendAndCheckCloudEventWithHeaders(testBridgeName, cloudEvent, path, parseHeaders(headers), responseCode);
    }

    private void sendAndCheckCloudEvent(String testBridgeName, String cloudEvent, String path, int responseCode) {
        String testCloudEventId = getCloudEventId(cloudEvent);
        context.getBridge(testBridgeName).storeCloudEventInContext(testCloudEventId);

        cloudEvent = adjustCloudEventParameters(cloudEvent, context.getBridge(testBridgeName).getCloudEventSystemId(testCloudEventId));

        sendAndCheckCloudEvent(testBridgeName, cloudEvent, path, new Headers(), responseCode);
    }

    private void sendAndCheckCloudEventWithHeaders(String testBridgeName, String cloudEvent, String path, Headers headers, int responseCode) {
        List<Header> patchedHeaders = new ArrayList<>();
        for (Header header : headers.asList()) {
            if (header.getName().equals("ce-id")) {
                // Replace test Cluster ID with system one and store the ID in context
                String testCloudEventId = header.getValue();
                context.getBridge(testBridgeName).storeCloudEventInContext(testCloudEventId);
                patchedHeaders.add(new Header(header.getName(), context.getBridge(testBridgeName).getCloudEventSystemId(testCloudEventId)));
            } else {
                patchedHeaders.add(header);
            }
        }
        // Add optional timestamp header
        patchedHeaders.add(new Header("ce-time", Instant.now().toString()));

        sendAndCheckCloudEvent(testBridgeName, cloudEvent, path, new Headers(patchedHeaders), responseCode);
    }

    private void sendAndCheckCloudEvent(String testBridgeName, String cloudEvent, String path, Headers headers,
                                        int responseCode) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName);
        endpoint = endpoint + "/" + path;

        String token = context.getManagerToken();
        try (InputStream cloudEventStream = new ByteArrayInputStream(cloudEvent.getBytes(StandardCharsets.UTF_8))) {
            IngressResource.postCloudEventResponse(token, endpoint, cloudEventStream, headers)
                    .then()
                    .statusCode(responseCode);
        } catch (IOException e) {
            throw new RuntimeException("Error with inputstream", e);
        }
    }

    private String getCloudEventId(String cloudEvent) {
        ObjectNode ce;
        try {
            ce = (ObjectNode) CloudEventUtils.getMapper().readTree(cloudEvent);
            return ce.get("id") == null ? null : ce.get("id").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing cloud event JSON content", e);
        }
    }

    private String adjustCloudEventParameters(String cloudEvent, String newCloudEventId) {
        ObjectNode ce;
        try {
            ce = (ObjectNode) CloudEventUtils.getMapper().readTree(cloudEvent);
            ce.put("id", newCloudEventId);
            ce.put("time", Instant.now().toString());
            return ce.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing cloud event JSON content", e);
        }
    }

    private Headers parseHeaders(String headers) {
        List<Header> parsedHeaders = new ArrayList<>();

        for (String header : headers.split(",")) {
            String[] headerValues = header.split(":");
            if (headerValues.length != 2) {
                throw new RuntimeException("Expected just header name and value separated by `:`, got: " + header);
            }
            parsedHeaders.add(new Header(headerValues[0].replaceAll("\"", ""), headerValues[1].replaceAll("\"", "")));
        }

        return new Headers(parsedHeaders);
    }
}
