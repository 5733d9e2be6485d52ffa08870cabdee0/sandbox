package com.redhat.service.bridge.processor.actions.webhook;

import com.redhat.service.bridge.actions.ActionProvider;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WebhookAction implements ActionProvider {

    public static final String TYPE = "Webhook";
    public static final String ENDPOINT_PARAM = "endpoint";
    public static final String USE_TECHNICAL_BEARER_TOKEN = "useTechnicalBearerToken";

    @Override
    public String getType() {
        return TYPE;
    }
}
