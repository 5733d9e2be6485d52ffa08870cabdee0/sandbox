package com.redhat.service.rhose.processor.actions.webhook;

import com.redhat.service.rhose.processor.actions.ActionBean;

public interface WebhookAction extends ActionBean {

    String TYPE = "Webhook";
    String ENDPOINT_PARAM = "endpoint";
    String USE_TECHNICAL_BEARER_TOKEN_PARAM = "useTechnicalBearerToken";

    @Override
    default String getType() {
        return TYPE;
    }
}
