package com.redhat.service.bridge.processor.actions.webhook;

import com.redhat.service.bridge.processor.actions.common.ActionAccepter;

public interface WebhookAction extends ActionAccepter {

    String TYPE = "Webhook";
    String ENDPOINT_PARAM = "endpoint";
    String USE_TECHNICAL_BEARER_TOKEN = "useTechnicalBearerToken";

    @Override
    default String getType() {
        return TYPE;
    }
}
