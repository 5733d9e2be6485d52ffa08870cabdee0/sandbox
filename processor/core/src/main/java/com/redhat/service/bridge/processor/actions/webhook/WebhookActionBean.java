package com.redhat.service.bridge.processor.actions.webhook;

import com.redhat.service.bridge.processor.actions.ActionBean;

public interface WebhookActionBean extends ActionBean {

    String TYPE = "Webhook";
    String ENDPOINT_PARAM = "endpoint";
    String USE_TECHNICAL_BEARER_TOKEN_PARAM = "useTechnicalBearerToken";

    @Override
    default String getType() {
        return TYPE;
    }
}
