package com.redhat.service.smartevents.integration.tests.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;

public class MetricsConverter {

    private static final Pattern METRICS_EXPRESSION = Pattern.compile("([a-z0-9_]+(?:\\{.*\\})?) (.*)(?: .*)?");

    private MetricsConverter() {
    }

    public static JsonObject convertToJson(String metrics) {
        JsonObject convertedMetrics = new JsonObject();
        for (String line : metrics.split(System.lineSeparator())) {
            if (line.startsWith("#")) {
                // Skip comments
                continue;
            }
            Matcher matcher = METRICS_EXPRESSION.matcher(line);
            if (!matcher.matches()) {
                throw new RuntimeException("Metrics line doesn't match the expected format: " + line);
            }
            int groupCount = matcher.groupCount();
            if (groupCount < 2 || groupCount > 3) {
                throw new RuntimeException("Expected 2 or 3 values separated by space in metrics line: " + line);
            }
            convertedMetrics.addProperty(matcher.group(1), matcher.group(2));
        }
        return convertedMetrics;
    }
}
