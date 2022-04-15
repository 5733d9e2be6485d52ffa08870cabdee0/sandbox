package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

/**
 * Main resolver class serving as entry point for all placeholder resolving. Calls other resolver implementations.
 */
public class ContextResolver {
    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("\\$\\{.*\\}");

    public static String resolveWithScenarioContext(TestContext context, String content) {
        if (isPlaceholderFound(content)) {
            if (CloudEventIdResolver.matchCloudEventId(content)) {
                content = CloudEventIdResolver.replaceCloudEventId(content, context);
            }
            if (SystemPropertyResolver.matchSystemProperty(content)) {
                content = SystemPropertyResolver.replaceSystemProperty(content, context);
            }
        }
        verifyNoPlaceholderAvailableInContent(content);
        return content;
    }

    private static boolean isPlaceholderFound(String content) {
        return PLACEHOLDER_REGEX.matcher(content).find();
    }

    private static void verifyNoPlaceholderAvailableInContent(String content) {
        Matcher matcher = PLACEHOLDER_REGEX.matcher(content);
        if (matcher.find()) {
            throw new RuntimeException("Found unresolved placeholder: " + matcher.group());
        }
    }
}
