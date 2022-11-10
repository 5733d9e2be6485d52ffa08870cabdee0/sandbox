package com.redhat.service.smartevents.shard.operator.v1.providers;

public interface GlobalConfigurationsProvider {
    String getSsoUrl();

    String getSsoClientId();

    String getSsoWebhookClientId();

    String getSsoWebhookClientSecret();

    String getSsoWebhookClientAccountId();

    Boolean isJsonLoggingEnabled();
}
