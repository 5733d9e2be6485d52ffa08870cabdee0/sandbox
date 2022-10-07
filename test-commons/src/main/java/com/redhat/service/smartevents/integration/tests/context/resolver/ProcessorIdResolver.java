package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

public class ProcessorIdResolver implements Resolver {

    private static final Pattern PROCESSOR_ID_REGEX = Pattern.compile("\\$\\{bridge\\.([^\\.]+)\\.processor\\.([^\\.]+)\\.id\\}");

    public boolean match(String placeholder) {
        return PROCESSOR_ID_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = PROCESSOR_ID_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String testBridgeName = match.group(1);
            String testProcessorName = match.group(2);
            return context.getBridge(testBridgeName).getProcessor(testProcessorName).getId();
        });
    }
}
