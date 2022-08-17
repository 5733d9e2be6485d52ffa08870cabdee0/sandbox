package com.redhat.service.smartevents.integration.tests.context;

import java.util.HashMap;
import java.util.Map;

public class PerfTestContext extends TestContext {

    private final Map<String, String> benchmarkRuns = new HashMap<>();

    public String getBenchmarkRun(String perfTestName) {
        return benchmarkRuns.get(perfTestName);
    }

    public void addBenchmarkRun(String perfTestName, String benchmarkRunId) {
        this.benchmarkRuns.put(perfTestName, benchmarkRunId);
    }
}
