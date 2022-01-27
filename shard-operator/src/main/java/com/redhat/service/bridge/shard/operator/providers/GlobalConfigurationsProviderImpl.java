package com.redhat.service.bridge.shard.operator.providers;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GlobalConfigurationsProviderImpl implements GlobalConfigurationsProvider {

    @ConfigProperty(name = "event-bridge.sso.auth-server-url")
    String ssoUrl;

    @ConfigProperty(name = "event-bridge.sso.client-id")
    String ssoClientId;

    @Override
    public String getSsoUrl() {
        return ssoUrl;
    }

    @Override
    public String getSsoClientId() {
        return ssoClientId;
    }
}
