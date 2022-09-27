package com.redhat.service.smartevents.integration.tests.common;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EndPointParser {

    public static final Pattern ENDPOINT_URL_REGEX = Pattern.compile("^(https?:\\/\\/[^/?#]+)([a-z0-9\\-._~%!$&'()*+,;=:@/]*)");

    private EndPointParser() {
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
