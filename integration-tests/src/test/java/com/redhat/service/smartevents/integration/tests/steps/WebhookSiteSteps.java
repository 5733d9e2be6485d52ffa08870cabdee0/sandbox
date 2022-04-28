package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.common.ChronoUnitConverter;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
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
                .untilAsserted(() -> assertThat(WebhookSiteResource.requests())
                        .map(request -> request.getContent())
                        .as("Searching for request containing text: '%s'",
                                requestTextWithoutPlaceholders)
                        .anyMatch(requestContent -> requestContent.contains(requestTextWithoutPlaceholders)));
    }

    @Then("^Webhook site does not contains request with text \"([^\"]*)\" within (\\d+) (second|seconds|minute|minutes)$")
    public void webhookSiteDoesNotContainsRequest(String requestText, long timeoutSeconds, String chronoUnits) throws InterruptedException {
        String requestTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, requestText);
        Instant timeoutTime = Instant.now().plus(Duration.ofSeconds(timeoutSeconds));
        while (timeoutTime.isAfter(Instant.now())) {
            ChronoUnit parsedChronoUnits = ChronoUnitConverter.parseChronoUnits(chronoUnits);
            TimeUnit.of(parsedChronoUnits).sleep(timeoutSeconds);
            assertThat(WebhookSiteResource.requests())
                    .map(WebhookSiteRequest::getContent)
                    .as("Checking that WebHook site doesn't contain request containing text: '%s'", requestTextWithoutPlaceholders)
                    .noneMatch(requestContent -> requestContent.contains(requestTextWithoutPlaceholders));
        }
    }
}
