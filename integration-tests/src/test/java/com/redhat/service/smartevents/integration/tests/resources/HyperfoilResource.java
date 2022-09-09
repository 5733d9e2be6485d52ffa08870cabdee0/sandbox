package com.redhat.service.smartevents.integration.tests.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import software.tnb.hyperfoil.validation.HyperfoilValidation;
import software.tnb.hyperfoil.validation.generated.ApiException;
import software.tnb.hyperfoil.validation.generated.model.Phase;
import software.tnb.hyperfoil.validation.generated.model.RequestStatisticsResponse;
import software.tnb.hyperfoil.validation.generated.model.Run;

public class HyperfoilResource {

    private static final String HYPERFOIL_URL = System.getProperty("performance.hyperfoil.url");
    private static final String BENCHMARK_CONTENT_FILE = "benchmark.hf.yaml";

    // Creating validation object directly as TNB Hyperfoil default service starts/stops Hyperfoil, causing tests to fail on environment without Docker environment
    public static HyperfoilValidation validation = new HyperfoilValidation(HYPERFOIL_URL);

    public static void addBenchmark(String benchmarkContent, String contentType) {
        // Currently TNB expects the benchmark to be provided either on classpath or as URL, so storing the benchmark content in dedicated file
        try {
            URL benchmarkResource = HyperfoilResource.class.getClassLoader().getResource(BENCHMARK_CONTENT_FILE);
            Path benchmarkResourcePath = Paths.get(benchmarkResource.toURI());
            Files.writeString(benchmarkResourcePath, benchmarkContent);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Exception while storing benchmark content into file on classpath", e);
        }

        validation.addBenchmark(BENCHMARK_CONTENT_FILE);
    }

    public static String getCompleteRun(String idRun) {
        Run run = validation.getRun(idRun);
        return run.toString();
    }

    public static Object getAllStatsJson(String idRun) {
        try {
            return validation.getDefaultApi().getAllStatsJson(idRun);
        } catch (ApiException e) {
            throw new RuntimeException("error while invoking Hyperfoil API", e);
        }
    }

    public static String runBenchmark(String perfTestName) {
        Run run = validation.runBenchmark(perfTestName);
        return run.getId();
    }

    public static boolean isRunCompleted(String idRun) {
        Run run = validation.getRun(idRun);
        return run.getCompleted();
    }

    public static boolean containsFailedRunPhase(String idRun) {
        Run run = validation.getRun(idRun);
        return run.getPhases().stream().anyMatch(Phase::getFailed);
    }

    public static int getTotalRequestsSent(String idRun, String phase, String metric) {
        try {
            RequestStatisticsResponse totalStats = validation.getDefaultApi().getTotalStats(idRun);
            List<Double> requestCounts = totalStats.getStatistics().stream().filter(s -> Objects.equals(s.getPhase(), phase) && Objects.equals(s.getMetric(), metric))
                    .flatMap(map -> ((Map<String, Object>) map.getSummary()).entrySet().stream()).filter(entry -> Objects.equals(entry.getKey(), "requestCount"))
                    .map(entry -> (Double) entry.getValue())
                    .collect(Collectors.toList());

            if (requestCounts.size() != 1) {
                throw new RuntimeException("Unable to resolve 'requestCount' from the stats json response.\n" +
                        "There should be just one and only one 'httpRequest' defined");
            }

            return requestCounts.get(0).intValue();
        } catch (ApiException e) {
            throw new RuntimeException("error while invoking Hyperfoil API", e);
        }
    }
}
