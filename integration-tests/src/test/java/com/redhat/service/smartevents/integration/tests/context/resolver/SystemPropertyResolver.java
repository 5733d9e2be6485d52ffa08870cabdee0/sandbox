package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

public class SystemPropertyResolver {

    private static final Pattern SYSTEM_PROPERTY_REGEX = Pattern.compile("\\$\\{env\\.([^\\}]+)\\}");

    static boolean matchSystemProperty(String placeholder) {
        return SYSTEM_PROPERTY_REGEX.matcher(placeholder).find();
    }

    static String replaceSystemProperty(String content, TestContext context) {
        Matcher matcher = SYSTEM_PROPERTY_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String systPropertyName = match.group(1);
            String systProperty = System.getProperty(systPropertyName);
            if (systProperty == null) {
                throw new RuntimeException("System property '" + systPropertyName + "' not found.");
            }
            return systProperty;
        });
    }
}
