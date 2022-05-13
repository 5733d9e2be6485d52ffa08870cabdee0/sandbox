package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.common.ChronoUnitConverter;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteQuerySorting;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteRequest;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteResource;

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
                .untilAsserted(() -> assertThat(WebhookSiteResource.requests(WebhookSiteQuerySorting.NEWEST))
                        .map(request -> request.getContent())
                        .as("Searching for request containing text: '%s'",
                                requestTextWithoutPlaceholders)
                        .anyMatch(requestContent -> requestContent.contains(requestTextWithoutPlaceholders)));
    }

    @Then("^Webhook site does not contains request with text \"([^\"]*)\" within (\\d+) (second|seconds|minute|minutes)$")
    public void webhookSiteDoesNotContainsRequest(String requestText, long timeoutAmount, String timeoutChronoUnits) throws InterruptedException {
        String requestTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, requestText);
        ChronoUnit parsedTimeoutChronoUnits = ChronoUnitConverter.parseChronoUnits(timeoutChronoUnits);
        Instant timeoutTime = Instant.now().plus(Duration.of(timeoutAmount, parsedTimeoutChronoUnits));
        while (timeoutTime.isAfter(Instant.now())) {
            TimeUnit.of(ChronoUnit.SECONDS).sleep(1);
            assertThat(WebhookSiteResource.requests(WebhookSiteQuerySorting.NEWEST))
                    .map(WebhookSiteRequest::getContent)
                    .as("Checking that WebHook site doesn't contain request containing text: '%s'", requestTextWithoutPlaceholders)
                    .noneMatch(requestContent -> requestContent.contains(requestTextWithoutPlaceholders));
        }
    }
}
