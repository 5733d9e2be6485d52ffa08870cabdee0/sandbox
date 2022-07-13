package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

/**
 * Main resolver class serving as entry point for all placeholder resolving.
 * Calls other resolver implementations.
 */
public class ContextResolver {
    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("\\$\\{.*\\}");
    private static final boolean UNDEFINED_PLACEHOLDER_CHECK_ENABLED = Boolean.getBoolean("undefined.placeholder.check.enabled");

    private static final List<Resolver> RESOLVERS = Arrays.asList(
            new BridgeEndpointBaseResolver(),
            new BridgeEndpointPathResolver(),
            new BridgeIdResolver(),
            new CloudEventIdResolver(),
            new ManagerAuthenticationTokenResolver(),
            new SystemPropertyResolver(),
            new UuidResolver());

    public static String resolveWithScenarioContext(TestContext context, String content) {
        if (isPlaceholderFound(content)) {
            for (Resolver resolver : RESOLVERS) {
                if (resolver.match(content)) {
                    content = resolver.replace(content, context);
                }
            }
        }
        if (UNDEFINED_PLACEHOLDER_CHECK_ENABLED) {
            verifyNoPlaceholderAvailableInContent(content);
        }
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
