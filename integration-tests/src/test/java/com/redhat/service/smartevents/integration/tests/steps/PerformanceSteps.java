package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.smartevents.integration.tests.common.AwaitilityOnTimeOutHandler;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.PerformanceResource;
import com.redhat.service.smartevents.integration.tests.resources.webhook.performance.WebhookPerformanceResource;

import io.cucumber.docstring.DocString;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class PerformanceSteps {

    private final TestContext context;

    public PerformanceSteps(TestContext context) {
        this.context = context;
    }

    @When("^Create benchmark on Hyperfoil \"([^\"]*)\" instance with content:$")
    public void createBenchmarkOnHyperfoilWithContent(String hfInstance, DocString benchmarkRequest) {
        String resolvedBenchmarkRequest = ContextResolver.resolveWithScenarioContext(context, benchmarkRequest.getContent());

        context.getScenario().log("Benchmark created as below\n\"" + resolvedBenchmarkRequest + "\n\"");
        PerformanceResource.addBenchmark(context.getManagerToken(), resolvedBenchmarkRequest, benchmarkRequest.getContentType());
    }

    @Then("^Run benchmark \"([^\"]*)\" on Hyperfoil \"([^\"]*)\" instance within (\\d+) (?:minute|minutes)$")
    public void runBenchmarkOnHyperfoilWithinMinutes(String perfTestName, String hfInstance, int timeoutMinutes) {
        String runId = PerformanceResource.runBenchmark(context.getManagerToken(), perfTestName);
        context.getScenario().log("Running benchmark ID " + runId);

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(() -> PerformanceResource
                        .getRunStatusDetailsResponse(context.getManagerToken(), runId).then().log().all()))
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(15))
                .untilAsserted(
                        () -> PerformanceResource
                                .getRunStatusDetailsResponse(context.getManagerToken(), runId)
                                .then()
                                .statusCode(200)
                                .body("completed", Matchers.is(true)));
    }

    @And("^number of cloud events sent is greater than (\\d+)$")
    public void numberOfCloudEventsIsGreaterThan(int events) {
        assertThat(WebhookPerformanceResource.getAllEventsCount()).isGreaterThan(events);
    }
}
