package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.SlackResource;

public class SlackChannelWebHookUrlResolver implements Resolver {

    private static final Pattern SLACK_CHANNEL_WEBHOOK_URL_REGEX = Pattern.compile("\\$\\{slack\\.channel\\.([^\\.]+)\\.webhook\\.url\\}");

    public boolean match(String placeholder) {
        return SLACK_CHANNEL_WEBHOOK_URL_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = SLACK_CHANNEL_WEBHOOK_URL_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String channelName = match.group(1);
            return SlackResource.account().webhookUrl(channelName);
        });
    }
}