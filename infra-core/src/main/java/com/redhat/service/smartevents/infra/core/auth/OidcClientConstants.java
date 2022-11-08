package com.redhat.service.smartevents.infra.core.auth;

public class OidcClientConstants {
    private OidcClientConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String WEBHOOK_OIDC_CLIENT_NAME = "webhook-oidc-client";
}
