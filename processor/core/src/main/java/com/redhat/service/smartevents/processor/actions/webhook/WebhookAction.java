package com.redhat.service.smartevents.processor.actions.webhook;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface WebhookAction extends GatewayBean {

    String TYPE = "Webhook";
    String ENDPOINT_PARAM = "endpoint";
    String USE_TECHNICAL_BEARER_TOKEN_PARAM = "useTechnicalBearerToken";
    String BASIC_AUTH_USERNAME_PARAM = "basic_auth_username";
    String BASIC_AUTH_PASSWORD_PARAM = "basic_auth_password";
    String SSL_VERIFICATION_DISABLED = "ssl_verification_disabled";

    @Override
    default String getType() {
        return TYPE;
    }
}
