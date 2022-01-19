package com.redhat.service.bridge.actions;

public class GlobalConfig {

    private String webhookTechnicalBearerToken;

    public GlobalConfig() {
    }

    public GlobalConfig withWebhookTecnicalBearerToken(String token) {
        this.webhookTechnicalBearerToken = token;
        return this;
    }

    public String getWebhookTechnicalBearerToken() {
        return webhookTechnicalBearerToken;
    }
}
