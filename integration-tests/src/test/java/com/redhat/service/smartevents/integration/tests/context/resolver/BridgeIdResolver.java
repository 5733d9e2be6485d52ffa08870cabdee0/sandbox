package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

public class BridgeIdResolver implements Resolver {

    private static final Pattern BRIDGE_ID_REGEX = Pattern.compile("\\$\\{bridge\\.([^\\.]+)\\.id\\}");

    public boolean match(String placeholder) {
        return BRIDGE_ID_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = BRIDGE_ID_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String testBridgeName = match.group(1);
            return context.getBridge(testBridgeName).getId();
        });
    }
}
