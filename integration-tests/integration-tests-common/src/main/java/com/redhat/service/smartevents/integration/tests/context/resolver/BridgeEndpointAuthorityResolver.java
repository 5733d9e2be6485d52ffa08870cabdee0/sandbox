package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

public class BridgeEndpointAuthorityResolver implements Resolver {

    private static final Pattern BRIDGE_ENDPOINT_AUTHORITY_REGEX = Pattern.compile("\\$\\{bridge\\.([^\\.]+)\\.endpoint\\.authority\\}");

    public boolean match(String placeholder) {
        return BRIDGE_ENDPOINT_AUTHORITY_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = BRIDGE_ENDPOINT_AUTHORITY_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String testBridgeName = match.group(1);
            return context.getBridge(testBridgeName).getEndPointAuthority();
        });
    }
}