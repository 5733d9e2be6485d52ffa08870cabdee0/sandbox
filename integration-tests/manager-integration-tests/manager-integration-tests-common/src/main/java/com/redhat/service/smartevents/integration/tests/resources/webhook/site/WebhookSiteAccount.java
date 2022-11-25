package com.redhat.service.smartevents.integration.tests.resources.webhook.site;

import software.tnb.common.account.AccountFactory;
import software.tnb.webhook.account.WebhookAccount;

public class WebhookSiteAccount {

    private WebhookSiteAccount() {
    }

    public static String getToken(String tokenName) {
        return AccountFactory.create(WebhookAccount.class).token(tokenName);
    }
}
