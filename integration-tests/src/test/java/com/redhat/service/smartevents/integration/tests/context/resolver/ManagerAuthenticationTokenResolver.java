package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

public class ManagerAuthenticationTokenResolver implements Resolver {

    private static final Pattern MANAGER_AUTHENTICATION_TOKEN_REGEX = Pattern.compile("\\$\\{manager\\.authentication\\.token\\}");

    public boolean match(String placeholder) {
        return MANAGER_AUTHENTICATION_TOKEN_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = MANAGER_AUTHENTICATION_TOKEN_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            return context.getManagerToken();
        });
    }
}
