package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.AwsAccount;

public class AwsRegionResolver implements Resolver {

    private static final Pattern AWS_REGION_REGEX = Pattern.compile("\\$\\{aws\\.region\\}");

    public boolean match(String placeholder) {
        return AWS_REGION_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = AWS_REGION_REGEX.matcher(content);
        return matcher.replaceAll(match -> AwsAccount.getRegion());
    }

}
