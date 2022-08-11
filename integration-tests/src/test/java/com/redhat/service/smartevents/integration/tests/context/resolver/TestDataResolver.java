package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

public class TestDataResolver implements Resolver {

    private static final Pattern TEST_DATA_PROPERTY_REGEX = Pattern.compile("\\$\\{data\\.([^\\}]+)\\}");

    @Override
    public boolean match(String placeholder) {
        return TEST_DATA_PROPERTY_REGEX.matcher(placeholder).find();
    }

    @Override
    public String replace(String content, TestContext context) {
        Matcher testDataMatcher = TEST_DATA_PROPERTY_REGEX.matcher(content);
        return testDataMatcher.replaceAll(matchResult -> {
            String key = matchResult.group(1);
            String value = context.getTestData(key);
            if (value == null) {
                throw new RuntimeException("Test data value for key: '" + key + "' was not found");
            }
            return value;
        });
    }
}
