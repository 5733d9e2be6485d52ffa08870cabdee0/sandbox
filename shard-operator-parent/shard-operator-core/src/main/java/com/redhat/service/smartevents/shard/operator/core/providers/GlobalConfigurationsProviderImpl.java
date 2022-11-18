package com.redhat.service.smartevents.shard.operator.core.providers;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GlobalConfigurationsProviderImpl implements GlobalConfigurationsProvider {

    @ConfigProperty(name = "event-bridge.sso.auth-server-url")
    String ssoUrl;

    @ConfigProperty(name = "event-bridge.sso.client-id")
    String ssoClientId;

    @ConfigProperty(name = "event-bridge.webhook.client-id")
    String webhookClientId;

    @ConfigProperty(name = "event-bridge.webhook.client-secret")
    String webhookClientSecret;

    @ConfigProperty(name = "event-bridge.webhook.account-id")
    String webhookAccountId;

    @ConfigProperty(name = "event-bridge.logging.json", defaultValue = "true")
    Boolean isJsonLoggingEnabled;

    @Override
    public String getSsoUrl() {
        return ssoUrl;
    }

    @Override
    public String getSsoClientId() {
        return ssoClientId;
    }

    @Override
    public String getSsoWebhookClientId() {
        return webhookClientId;
    }

    @Override
    public String getSsoWebhookClientSecret() {
        return webhookClientSecret;
    }

    @Override
    public String getSsoWebhookClientAccountId() {
        return webhookAccountId;
    }

    @Override
    public Boolean isJsonLoggingEnabled() {
        return isJsonLoggingEnabled;
    }
}
