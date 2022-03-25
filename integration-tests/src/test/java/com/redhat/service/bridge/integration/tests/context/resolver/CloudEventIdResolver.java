package com.redhat.service.bridge.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.bridge.integration.tests.context.TestContext;

public class CloudEventIdResolver {

    private static final Pattern CLOUD_EVENT_ID_REGEX = Pattern.compile("\\$\\{bridge\\.([^\\.]+)\\.cloud-event\\.([^\\.]+)\\.id\\}");

    static boolean matchCloudEventId(String placeholder) {
        return CLOUD_EVENT_ID_REGEX.matcher(placeholder).find();
    }

    static String replaceCloudEventId(String content, TestContext context) {
        Matcher matcher = CLOUD_EVENT_ID_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String testBridgeName = match.group(1);
            String testCloudEventId = match.group(2);

            return context.getBridge(testBridgeName).getCloudEventSystemId(testCloudEventId);
        });
    }
}
