package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.common.AwaitilityOnTimeOutHandler;
import com.redhat.service.smartevents.integration.tests.context.PerfTestContext;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.HyperfoilResource;
import com.redhat.service.smartevents.integration.tests.resources.webhook.performance.WebhookPerformanceResource;

import io.cucumber.datatable.DataTable;
import io.cucumber.docstring.DocString;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class PerformanceSteps {

    private final PerfTestContext perfContext;
    private final TestContext context;

    public PerformanceSteps(TestContext context, PerfTestContext perfContext) {
        this.context = context;
        this.perfContext = perfContext;
    }

    @When("^create benchmark with content:$")
    public void createBenchmarkOnHyperfoilWithContent(DocString benchmarkRequest) {
        String resolvedBenchmarkRequest = ContextResolver.resolveWithScenarioContext(context, benchmarkRequest.getContent());

        context.getScenario().log("Benchmark created as below\n\"" + resolvedBenchmarkRequest + "\n\"");
        HyperfoilResource.addBenchmark(resolvedBenchmarkRequest, benchmarkRequest.getContentType());
    }

    @Then("^run benchmark \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void runBenchmarkOnHyperfoilWithinMinutes(String perfTestName, int timeoutMinutes) {
        String runId = HyperfoilResource.runBenchmark(perfTestName);
        perfContext.addBenchmarkRun(perfTestName, runId);
        context.getScenario().log("Running benchmark ID " + perfContext.getBenchmarkRun(perfTestName));

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(() -> context.getScenario().log("Unfinished performance run: " + HyperfoilResource.getCompleteRun(runId))))
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> assertThat(HyperfoilResource.isRunCompleted(runId))
                                .as("Waiting for performance run to finish")
                                .isTrue());
    }

    @And("^the benchmark run \"([^\"]*)\" was executed successfully$")
    public void benchmarkExecutionWasSuccessfully(String perfTestName) {
        if (perfContext.getBenchmarkRun(perfTestName) == null) {
            throw new RuntimeException("there is no benchmark run executed for " + perfTestName + " scenario");
        }

        assertThat(HyperfoilResource.containsFailedRunPhase(perfContext.getBenchmarkRun(perfTestName)))
                .as("Checking if benchmark run contains failed phases")
                .isFalse();
    }

    @And("^the total of events received for benchmark \"([^\"]*)\" run of Bridge \"([^\"]*)\" is equal to the total of cloud events sent in:$")
    public void numberOfEventsReceivedIsEqualToEventsSent(String perfTestName, String bridgeName, DataTable parametersDatatable) {
        String bridgeId = context.getBridge(bridgeName).getId();
        Integer totalEventsReceived = WebhookPerformanceResource.getCountEventsReceived(bridgeId, Integer.class);
        int totalEventsSent = HyperfoilResource.getTotalRequestsSent(perfContext.getBenchmarkRun(perfTestName), parametersDatatable.asMap().get("phase"), parametersDatatable.asMap().get("metric"));

        assertThat(totalEventsReceived)
                .isEqualTo(totalEventsSent)
                .isPositive();
    }
}
