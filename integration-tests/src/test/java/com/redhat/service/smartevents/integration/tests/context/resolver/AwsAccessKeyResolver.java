package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.AwsAccount;

public class AwsAccessKeyResolver implements Resolver {

    private static final Pattern AWS_ACCESS_KEY_REGEX = Pattern.compile("\\$\\{aws\\.access-key\\}");

    public boolean match(String placeholder) {
        return AWS_ACCESS_KEY_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = AWS_ACCESS_KEY_REGEX.matcher(content);
        return matcher.replaceAll(match -> AwsAccount.getAccessKey());
    }

}
