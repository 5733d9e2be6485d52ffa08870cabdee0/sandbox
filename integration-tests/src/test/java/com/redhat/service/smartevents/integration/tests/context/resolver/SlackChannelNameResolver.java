package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.SlackResource;

public class SlackChannelNameResolver implements Resolver {

    private static final Pattern SLACK_CHANNEL_NAME_REGEX = Pattern.compile("\\$\\{slack\\.channel\\.([^\\.]+)\\.name\\}");

    public boolean match(String placeholder) {
        return SLACK_CHANNEL_NAME_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = SLACK_CHANNEL_NAME_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String channelName = match.group(1);
            return SlackResource.account().channel(channelName);
        });
    }
}