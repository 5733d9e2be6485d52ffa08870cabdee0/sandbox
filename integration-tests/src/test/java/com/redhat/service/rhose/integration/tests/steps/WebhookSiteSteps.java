package com.redhat.service.rhose.integration.tests.steps;

import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;

import com.redhat.service.rhose.integration.tests.context.TestContext;
import com.redhat.service.rhose.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.rhose.integration.tests.resources.webhook.site.WebhookSiteResource;

import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class WebhookSiteSteps {

    private TestContext context;

    public WebhookSiteSteps(TestContext context) {
        this.context = context;
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
