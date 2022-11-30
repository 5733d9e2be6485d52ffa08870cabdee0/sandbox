package com.redhat.service.smartevents.integration.tests.context.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteAccount;

public class WebhookSiteTokenResolver implements Resolver {

    private static final Pattern WEBHOOK_SITE_TOKEN_REGEX = Pattern.compile("\\$\\{webhook\\.site\\.token\\.([^\\}]+)\\}");

    public boolean match(String placeholder) {
        return WEBHOOK_SITE_TOKEN_REGEX.matcher(placeholder).find();
    }

    public String replace(String content, TestContext context) {
        Matcher matcher = WEBHOOK_SITE_TOKEN_REGEX.matcher(content);
        return matcher.replaceAll(match -> {
            String tokenName = match.group(1);
            return WebhookSiteAccount.getToken(tokenName);
        });
    }
}
