package com.redhat.service.smartevents.integration.tests.resources;

import com.redhat.service.smartevents.integration.tests.common.Constants;

import io.restassured.response.Response;

public class HyperfoilResource {

    private static final String HYPERFOIL_URL = System.getProperty("performance.hyperfoil.url");
    private static final String BASE_BENCHMARK_URL = HYPERFOIL_URL + "/benchmark/";
    private static final String BASE_RUN_URL = HYPERFOIL_URL + "/run/";

    public static void addBenchmark(String managerToken, String benchmarkContent, String contentType) {
        createBenchmarkResponse(managerToken, benchmarkContent, contentType)
                .then()
                .log().ifValidationFails()
                .statusCode(204);
    }

    public static Response createBenchmarkResponse(String token, String benchmarkContent, String contentType) {
        return ResourceUtils.newRequest(token, contentType)
                .body(benchmarkContent)
                .post(BASE_BENCHMARK_URL);
    }

    public static Response getBenchmarkDetailsResponse(String token, String perfTestName) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .get(BASE_BENCHMARK_URL + perfTestName);
    }

    public static String runBenchmark(String token, String perfTestName) {
        return runBenchmarkResponse(token, perfTestName)
                .then()
                .log().ifValidationFails()
                .statusCode(202)
                .extract()
                .body()
                .jsonPath().getString("id");
    }

    public static Response runBenchmarkResponse(String token, String perfTestName) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .get(BASE_BENCHMARK_URL + perfTestName + "/start");
    }

    public static Response getRunStatusDetailsResponse(String token, String runId) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .get(BASE_RUN_URL + runId);
    }

    public static Response getBenchmarkStatsDetailResponse(String token, String runId) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .get(BASE_RUN_URL + runId + "/stats/total");
    }
}
