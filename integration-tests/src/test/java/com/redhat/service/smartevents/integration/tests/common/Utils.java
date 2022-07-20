package com.redhat.service.smartevents.integration.tests.common;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern ENDPOINT_URL_REGEX = Pattern.compile("^(https?:\\/\\/[^/?#]+)([a-z0-9\\-._~%!$&'()*+,;=:@/]*)");

    public static String getSystemProperty(String parameters) {
        if (System.getProperty(parameters) == null || System.getProperty(parameters).isEmpty()) {
            throw new RuntimeException("Property " + parameters + " was not defined.");
        }
        return System.getProperty(parameters);
    }

    public static String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 4);
    }

    public static Optional<String> getEndpointBaseUrl(String endpoint) {
        Matcher matcher = ENDPOINT_URL_REGEX.matcher(endpoint);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public static Optional<String> getEndpointPathUrl(String endpoint) {
        Matcher matcher = ENDPOINT_URL_REGEX.matcher(endpoint);
        if (matcher.find()) {
            return Optional.of(matcher.group(2));
        }
        return Optional.empty();
    }
}
