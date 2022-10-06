package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.common.AwaitilityOnTimeOutHandler;
import com.redhat.service.smartevents.integration.tests.context.PerfTestContext;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.HorreumResource;
import com.redhat.service.smartevents.integration.tests.resources.HyperfoilResource;
import com.redhat.service.smartevents.integration.tests.resources.webhook.performance.WebhookPerformanceResource;

import io.cucumber.datatable.DataTable;
import io.cucumber.docstring.DocString;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class PerformanceSteps {

    private final PerfTestContext perfContext;
    private final TestContext context;

    public PerformanceSteps(TestContext context, PerfTestContext perfContext) {
        this.context = context;
        this.perfContext = perfContext;
    }

    @When("^run benchmark with content:$")
    public void createBenchmarkOnHyperfoilWithContent(DocString benchmarkRequest) {
        String resolvedBenchmarkRequest = ContextResolver.resolveWithScenarioContext(context, benchmarkRequest.getContent());

        context.getScenario().log("Benchmark created as below\n\"" + resolvedBenchmarkRequest + "\n\"");
        String perfTestName = HyperfoilResource.addBenchmark(resolvedBenchmarkRequest, benchmarkRequest.getContentType());

        String runId = HyperfoilResource.runBenchmark(perfTestName);
        perfContext.addBenchmarkRun(perfTestName, runId);
        context.getScenario().log("Running benchmark ID " + runId);

        // Wait until scenario execution finish, by default the timeout is specified as part of Hyperfoil scenario
        // In case of some issue with Hyperfoil timeout the hardcoded waiting time here is 4 hours.
        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(() -> context.getScenario().log("Unfinished performance run: " + HyperfoilResource.getCompleteRun(runId))))
                .atMost(Duration.ofHours(4L))
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
                .as("Checking if benchmark run contains failed phases: " + HyperfoilResource.getCompleteRun(perfContext.getBenchmarkRun(perfTestName)))
                .isFalse();
    }

    @And("^the total of events received for benchmark \"([^\"]*)\" run in \"([^\"]*)\" phase is equal to the total of cloud events sent in:$")
    public void numberOfEventsReceivedIsEqualToEventsSent(String perfTestName, String phase, DataTable parametersDatatable) {
        parametersDatatable.entries()
                .forEach(entry -> {
                    String runId = perfContext.getBenchmarkRun(perfTestName);
                    String bridgeId = context.getBridge(entry.get("bridge")).getId();
                    String metric = entry.get("metric");
                    Integer totalEventsReceived = WebhookPerformanceResource.getCountEventsReceived(bridgeId, Integer.class);
                    int totalEventsSent = HyperfoilResource.getTotalRequestsSent(runId, phase, metric);
                    assertThat(totalEventsReceived)
                            .isEqualTo(totalEventsSent)
                            .isPositive();
                });
    }

    @When("^store results of benchmark run \"([^\"]*)\" in Horreum test \"([^\"]*)\"$")
    public void storeResultsInHorreumTest(String perfTestName, String testName) {
        if (!HorreumResource.isResultsUploadEnabled()) {
            context.getScenario().log("Horreum results upload disabled. Skipping the step.");
            return;
        }

        String benchmarkRun = perfContext.getBenchmarkRun(perfTestName);
        String testDescription = context.getScenario().getName();
        HorreumResource.storePerformanceData(testName, testDescription, context.getStartTime(), benchmarkRun);
    }

    @When("^store Manager metrics in Horreum test \"([^\"]*)\"$")
    public void storeManagerMetricsInHorreumTest(String testName) {
        if (!HorreumResource.isResultsUploadEnabled()) {
            context.getScenario().log("Horreum results upload disabled. Skipping the step.");
            return;
        }

        String testDescription = context.getScenario().getName();
        HorreumResource.storeManagerPerformanceData(testName, testDescription, context.getStartTime());
    }
}
