package com.redhat.service.smartevents.integration.tests.steps;

import com.redhat.service.smartevents.integration.tests.common.BridgeUtils;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.ResourceUtils;

import io.cucumber.java.en.Given;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsSteps {

    private TestContext context;

    public MetricsSteps(TestContext context) {
        this.context = context;
    }

    @Given("^the Manager metric \'([^\']*)\' count is at least (\\d+)$")
    public void managerMetricCountIsAtLeast(String metricName, int minimalValue) {
        testMetricAndCount(BridgeUtils.MANAGER_URL + "/q/metrics", metricName, minimalValue);
    }

    @Given("^the Ingress of the Bridge \"([^\"]*)\" metric \'([^\']*)\' count is at least (\\d+)$")
    public void ingressOfBridgeMetricCountIsAtLeast(String testBridgeName, String metricName, int minimalValue) {
        testMetricAndCount(BridgeUtils.getOrRetrieveBridgeEndpoint(context, testBridgeName) + "/q/metrics", metricName, minimalValue);
    }

    private void testMetricAndCount(String metricsEndpoint, String metricName, int minimalValue) {
        String metrics = ResourceUtils.jsonRequest(context.getManagerToken())
                .get(metricsEndpoint)
                .then()
                .extract()
                .body()
                .asString();

        assertThat(metrics).contains(metricName);
        metrics.lines()
                .filter(l -> l.contains(metricName))
                .map(m -> m.replace(metricName + " ", ""))
                .mapToDouble(m -> Double.parseDouble(m))
                .forEach(d -> assertThat(d).as("Checking %s value", metricName).isGreaterThanOrEqualTo(minimalValue));
    }
}
