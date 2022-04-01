package com.redhat.service.bridge.shard.operator.providers;

public interface GlobalConfigurationsProvider {
    String getSsoUrl();

    String getSsoClientId();

    String getSsoWebhookClientId();

    String getSsoWebhookClientSecret();

    String getSsoWebhookClientAccountId();
}
