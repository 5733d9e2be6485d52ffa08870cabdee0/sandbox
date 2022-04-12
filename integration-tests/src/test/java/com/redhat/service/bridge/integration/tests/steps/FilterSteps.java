package com.redhat.service.bridge.integration.tests.steps;


import java.time.Duration;

import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.context.resolver.ContextResolver;

import com.redhat.service.bridge.integration.tests.resources.webhook.site.WebhookSiteResource;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class FilterSteps {
    private TestContext context;
    private final String KEY = "source";
    private final String VALUE = "StorageService";

    public FilterSteps(TestContext context) {
        this.context = context;
    }


    @When("^send a cloud event to the Ingress of the Bridge \"([^\"]*)\":$")
    public void sendCloudEventToIngressOfBridgeWithPath(String testBridgeName, String cloudEvent) {
        IngressSteps ingressSteps=new IngressSteps(context);
        ingressSteps.sendAndCheckCloudEvent(testBridgeName, cloudEvent, "", 200);
    }




    @Then("^Webhook site contains request with text \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void webhookSiteContainsRequest(String requestText, int timeoutMinutes) {
        String requestTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, requestText);
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> Assertions.assertThat(WebhookSiteResource.requests())
                        .map(request -> request.getContent())
                        .as("Searching for request containing text: '%s'",
                                requestTextWithoutPlaceholders)
                        .anyMatch(requestContent -> requestContent.contains(requestTextWithoutPlaceholders)));
    }
}


