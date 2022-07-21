package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

public class KafkaTopicResolver implements Resolver {

    private static final Pattern TOPIC_REGEX = Pattern.compile("\\$\\{topic\\.([^\\.]+)\\}");

    @Override
    public boolean match(String placeholder) {
        return TOPIC_REGEX.matcher(placeholder).find();
    }

    @Override
    public String replace(String content, TestContext context) {
        Matcher matcher = TOPIC_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String topicName = match.group(1);
            return context.getKafkaTopic(topicName);
        });
    }
}
