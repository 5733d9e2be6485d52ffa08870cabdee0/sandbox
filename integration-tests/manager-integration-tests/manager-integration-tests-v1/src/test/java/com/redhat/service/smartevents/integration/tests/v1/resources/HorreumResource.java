package com.redhat.service.smartevents.integration.tests.v1.resources;

import java.time.Instant;

import com.google.gson.JsonObject;
import com.redhat.service.smartevents.integration.tests.common.MetricsConverter;
import com.redhat.service.smartevents.integration.tests.common.Utils;
import com.redhat.service.smartevents.integration.tests.resources.HyperfoilResource;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

import software.tnb.common.service.ServiceFactory;
import software.tnb.horreum.service.Horreum;

public class HorreumResource {
    private static final boolean HORREUM_RESULTS_UPLOAD = Boolean.getBoolean("performance.horreum.results.upload");
    private static final String HORREUM_TEST_RESULT_SCOPE = "PUBLIC";

    private static String HORREUM_TEAM_NAME;
    private static String HORREUM_TEST_SCHEMA;
    private static String MANAGER_METRICS_TEST_SCHEMA;

    public static Horreum horreum = ServiceFactory.create(Horreum.class);

    // Manually triggering beforeAll and afterAll as these methods are intended to be triggered as JUnit5 Extension, however Cucumber support JUnit5 Extensions.

    @BeforeAll
    public static void beforeAll() throws Exception {
        if (isResultsUploadEnabled()) {
            horreum.beforeAll(null);
            HORREUM_TEAM_NAME = Utils.getSystemProperty("performance.horreum.team.name");
            HORREUM_TEST_SCHEMA = Utils.getSystemProperty("performance.horreum.test.schema");
            MANAGER_METRICS_TEST_SCHEMA = Utils.getSystemProperty("performance.manager.metrics.test.schema");
        }
    }

    @AfterAll
    public static void afterAll() throws Exception {
        if (isResultsUploadEnabled()) {
            horreum.afterAll(null);
        }
    }

    public static boolean isResultsUploadEnabled() {
        return HORREUM_RESULTS_UPLOAD;
    }

    public static void storePerformanceData(String testName, String testDescription, Instant startTime, String benchmarkRun) {
        Object totalStats = HyperfoilResource.getAllStatsJson(benchmarkRun);
        try {
            horreum.validation().postRunData(testDescription, HORREUM_TEAM_NAME, HORREUM_TEST_SCHEMA, startTime.toString(), Instant.now().toString(), testName, HORREUM_TEST_RESULT_SCOPE, totalStats);
        } catch (Exception e) {
            throw new RuntimeException("error while sending performance data to Horreum", e);
        }
    }

    public static void storeManagerPerformanceData(String testName, String testDescription, Instant startTime) {
        String managerMetrics = ManagerResource.getManagerMetrics();
        JsonObject convertedMetrics = MetricsConverter.convertToJson(managerMetrics);
        try {
            horreum.validation().postRunData(testDescription, HORREUM_TEAM_NAME, MANAGER_METRICS_TEST_SCHEMA, startTime.toString(), Instant.now().toString(), testName, HORREUM_TEST_RESULT_SCOPE,
                    convertedMetrics);
        } catch (Exception e) {
            throw new RuntimeException("error while sending performance data to Horreum", e);
        }
    }
}
