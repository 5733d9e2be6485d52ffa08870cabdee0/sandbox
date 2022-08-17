package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.smartevents.integration.tests.common.AwaitilityOnTimeOutHandler;
import com.redhat.service.smartevents.integration.tests.context.PerfTestContext;
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

    private final PerfTestContext context;

    public PerformanceSteps(PerfTestContext context) {
        this.context = context;
    }

    @When("^create benchmark with content:$")
    public void createBenchmarkOnHyperfoilWithContent(DocString benchmarkRequest) {
        String resolvedBenchmarkRequest = ContextResolver.resolveWithScenarioContext(context, benchmarkRequest.getContent());

        context.getScenario().log("Benchmark created as below\n\"" + resolvedBenchmarkRequest + "\n\"");
        HyperfoilResource.addBenchmark(context.getManagerToken(), resolvedBenchmarkRequest, benchmarkRequest.getContentType());
    }

    @Then("^run benchmark \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void runBenchmarkOnHyperfoilWithinMinutes(String perfTestName, int timeoutMinutes) {
        context.addBenchmarkRun(perfTestName, HyperfoilResource.runBenchmark(context.getManagerToken(), perfTestName));
        context.getScenario().log("Running benchmark ID " + context.getBenchmarkRun(perfTestName));

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(() -> HyperfoilResource
                        .getRunStatusDetailsResponse(context.getManagerToken(), context.getBenchmarkRun(perfTestName)).then().log().all()))
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(15))
                .untilAsserted(
                        () -> HyperfoilResource
                                .getRunStatusDetailsResponse(context.getManagerToken(), context.getBenchmarkRun(perfTestName))
                                .then()
                                .statusCode(200)
                                .body("completed", Matchers.is(true)));
    }

    @And("^the benchmark run \"([^\"]*)\" was executed successfully$")
    public void benchmarkExecutionWasSuccessfully(String perfTestName) {
        if (context.getBenchmarkRun(perfTestName) == null) {
            throw new RuntimeException("there is no benchmark run executed for " + perfTestName + " scenario");
        }
        assertThat(HyperfoilResource.getRunStatusDetailsResponse(context.getManagerToken(), context.getBenchmarkRun(perfTestName))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath().getList("phases", Map.class).stream()
                .filter(map -> ((Boolean) map.get("failed")))
                .count()).isZero();
    }

    @And("^the total of events received for benchmark \"([^\"]*)\" run of Bridge \"([^\"]*)\" is equal to the total of cloud events sent in:$")
    public void numberOfEventsReceivedIsEqualToEventsSent(String perfTestName, String bridgeName, DataTable parametersDatatable) {
        String bridgeId = context.getBridge(bridgeName).getId();
        Integer totalEventsReceived = WebhookPerformanceResource.getCountEventsReceived(bridgeId, Integer.class);
        List<Integer> totalEventsSent = HyperfoilResource.getBenchmarkStatsDetailResponse(context.getManagerToken(), context.getBenchmarkRun(perfTestName))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath().getList("statistics", Map.class).stream()
                .filter(map -> map.get("phase").equals(parametersDatatable.asMap().get("phase")) &&
                        map.get("metric").equals(parametersDatatable.asMap().get("metric")))
                .flatMap(map -> ((Map<String, Object>) map.get("summary")).entrySet().stream())
                .filter(entry -> entry.getKey().equals("requestCount"))
                .map(entry -> (Integer) entry.getValue())
                .collect(Collectors.toList());

        assertThat(totalEventsSent)
                .withFailMessage("Unable to resolve 'requestCount' from the stats json response.\n" +
                        "There should be just one and only one 'httpRequest' defined")
                .hasSize(1);

        assertThat(totalEventsReceived)
                .isEqualTo(totalEventsSent.get(0))
                .isPositive();
    }
}
