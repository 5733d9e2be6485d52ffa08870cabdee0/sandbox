package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.common.ChronoUnitConverter;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteQuerySorting;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteRequest;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteResource;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class WebhookSiteSteps {

    private TestContext context;

    public WebhookSiteSteps(TestContext context) {
        this.context = context;
    }

    @Then("^Webhook site with id \"(.*)\" contains request with text \"(.*)\" within (\\d+) (?:minute|minutes)$")
    public void webhookSiteWithIdContainsRequest(String webhookId, String requestText, int timeoutMinutes) {
        String requestTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, requestText);
        String webhookIdResolver = ContextResolver.resolveWithScenarioContext(context, webhookId);
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(WebhookSiteResource.requests(webhookIdResolver, WebhookSiteQuerySorting.NEWEST))
                        .map(request -> request.getContent())
                        .as("Searching for request containing text: '%s'",
                                requestTextWithoutPlaceholders)
                        .anyMatch(requestContent -> requestContent.contains(requestTextWithoutPlaceholders)));
    }

    @Then("^Webhook site with id \"(.*)\" contains request with text \"(.*)\" within (\\d+) (?:minute|minutes) and headers:$")
    public void webhookSiteWithIdContainsRequestWithHeaders(String webhookId, String requestText, int timeoutMinutes, DataTable parametersDatatable) {
        String requestTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, requestText);
        String webhookIdResolver = ContextResolver.resolveWithScenarioContext(context, webhookId);
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(WebhookSiteResource.requests(webhookIdResolver, WebhookSiteQuerySorting.NEWEST))
                        .as("Searching for request containing text: '%s'", requestTextWithoutPlaceholders)
                        .anySatisfy(request -> {
                            assertThat(request.getContent()).contains(requestTextWithoutPlaceholders);
                            assertHeadersInRequest(request, parametersDatatable);
                        }));
    }

    @Then("^Webhook site with id \"(.*)\" does not contains request with text \"([^\"]*)\" within (\\d+) (second|seconds|minute|minutes)$")
    public void webhookSiteWithIdDoesNotContainsRequest(String webhookId, String requestText, long timeoutAmount, String timeoutChronoUnits) throws InterruptedException {
        String requestTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, requestText);
        String webhookIdResolver = ContextResolver.resolveWithScenarioContext(context, webhookId);
        ChronoUnit parsedTimeoutChronoUnits = ChronoUnitConverter.parseChronoUnits(timeoutChronoUnits);
        Instant timeoutTime = Instant.now().plus(Duration.of(timeoutAmount, parsedTimeoutChronoUnits));
        while (timeoutTime.isAfter(Instant.now())) {
            TimeUnit.of(ChronoUnit.SECONDS).sleep(1);
            assertThat(WebhookSiteResource.requests(webhookIdResolver, WebhookSiteQuerySorting.NEWEST))
                    .map(WebhookSiteRequest::getContent)
                    .as("Checking that WebHook site doesn't contain request containing text: '%s'", requestTextWithoutPlaceholders)
                    .noneMatch(requestContent -> requestContent.contains(requestTextWithoutPlaceholders));
        }
    }

    private void assertHeadersInRequest(WebhookSiteRequest request, DataTable parametersDatatable) {
        Map<String, List<String>> headers = request.getHeaders();
        parametersDatatable.asMap().forEach((key, value) -> {
            String valueWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, value);
            assertThat(headers.get(key)).isNotNull();
            assertThat(headers.get(key)).hasSize(1);
            String actualValue = headers.get(key).get(0);
            if (valueWithoutPlaceholders != null && !valueWithoutPlaceholders.isEmpty()) {
                assertThat(actualValue).isEqualTo(valueWithoutPlaceholders);
            }
        });
    }
}
