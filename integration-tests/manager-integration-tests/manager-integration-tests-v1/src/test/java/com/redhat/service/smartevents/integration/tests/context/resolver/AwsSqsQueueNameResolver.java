package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

public class AwsSqsQueueNameResolver implements Resolver {

    private static final Pattern QUEUE_NAME_REGEX = Pattern.compile("\\$\\{aws\\.sqs\\.([^\\}]+)\\}");

    @Override
    public boolean match(String placeholder) {
        return QUEUE_NAME_REGEX.matcher(placeholder).find();
    }

    @Override
    public String replace(String content, TestContext context) {
        Matcher matcher = QUEUE_NAME_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String queueName = match.group(1);
            return context.getSqsQueue(queueName);
        });
    }
}
