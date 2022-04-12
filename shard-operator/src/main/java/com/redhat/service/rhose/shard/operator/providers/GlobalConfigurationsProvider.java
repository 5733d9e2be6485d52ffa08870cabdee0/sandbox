package com.redhat.service.rhose.shard.operator.providers;

public interface GlobalConfigurationsProvider {
    String getSsoUrl();

    String getSsoClientId();

    String getSsoWebhookClientId();

    String getSsoWebhookClientSecret();

    String getSsoWebhookClientAccountId();
}
