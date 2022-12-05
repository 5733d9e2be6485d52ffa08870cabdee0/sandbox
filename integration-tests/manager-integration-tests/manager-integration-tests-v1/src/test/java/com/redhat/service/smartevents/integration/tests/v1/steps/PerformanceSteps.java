package com.redhat.service.smartevents.integration.tests.v1.steps;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.redhat.service.smartevents.integration.tests.common.MetricsConverter;
import com.redhat.service.smartevents.integration.tests.context.PerfTestContext;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.HyperfoilResource;
import com.redhat.service.smartevents.integration.tests.v1.resources.ManagerResource;

import io.cucumber.java.en.When;

public class PerformanceSteps {

    private final PerfTestContext perfContext;
    private final TestContext context;

    public PerformanceSteps(TestContext context, PerfTestContext perfContext) {
        this.context = context;
        this.perfContext = perfContext;
    }

    @When("^store Manager metrics to json file \"([^\"]*)\"$")
    public void storeManagerMetricsToFile(String fileName) {
        try {
            String managerMetrics = ManagerResource.getManagerMetrics();
            JsonObject convertedMetrics = MetricsConverter.convertToJson(managerMetrics);
            Gson gson = new Gson();
            String convertedMetricsJson = gson.toJson(convertedMetrics);
            HyperfoilResource.storeToHyperfoilResultsFolder(fileName, convertedMetricsJson);
        } catch (IOException e) {
            context.getScenario().log(String.format("Failed to store Manager metrics into filesystem: %s", e.getMessage()));
        }
    }

}
