package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.SlackResource;

public class SlackTokenResolver implements Resolver {

    private static final Pattern SLACK_TOKEN_REGEX = Pattern.compile("\\$\\{slack\\.token\\}");

    public boolean match(String placeholder) {
        return SLACK_TOKEN_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = SLACK_TOKEN_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            return SlackResource.account().token();
        });
    }
}
