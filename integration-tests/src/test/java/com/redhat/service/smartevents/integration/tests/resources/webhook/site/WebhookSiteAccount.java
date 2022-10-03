package com.redhat.service.smartevents.integration.tests.resources.webhook.site;

import software.tnb.common.account.Accounts;
import software.tnb.webhook.account.WebhookAccount;

public class WebhookSiteAccount {

    private WebhookSiteAccount() {
    }

    public static String getToken(String tokenName) {
        return Accounts.get(WebhookAccount.class).token(tokenName);
    }
}
