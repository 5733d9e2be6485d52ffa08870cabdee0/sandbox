package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

public class UuidResolver implements Resolver {

    private static final Pattern UUID_REGEX = Pattern.compile("\\$\\{uuid\\.([^\\}]+)\\}");

    public boolean match(String placeholder) {
        return UUID_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = UUID_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String uuidName = match.group(1);
            return context.getUuid(uuidName);
        });
    }
}
