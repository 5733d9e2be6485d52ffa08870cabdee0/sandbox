package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

public class CloudEventIdResolver implements Resolver {

    private static final Pattern CLOUD_EVENT_ID_REGEX = Pattern.compile("\\$\\{cloud-event\\.([^\\.]+)\\.id\\}");

    public boolean match(String placeholder) {
        return CLOUD_EVENT_ID_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = CLOUD_EVENT_ID_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String testCloudEventId = match.group(1);

            return context.getCloudEventSystemId(testCloudEventId);
        });
    }
}
