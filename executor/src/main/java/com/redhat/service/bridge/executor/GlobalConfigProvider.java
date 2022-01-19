package com.redhat.service.bridge.executor;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.bridge.actions.GlobalConfig;

@Dependent
public class GlobalConfigProvider {

    @ConfigProperty(name = "event-bridge.webhook.technical-bearer-token")
    Optional<String> webhookTechnicalBearerToken;

    @Produces
    public GlobalConfig produceGlobalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        webhookTechnicalBearerToken.map(globalConfig::withWebhookTecnicalBearerToken);
        return globalConfig;
    }
}
