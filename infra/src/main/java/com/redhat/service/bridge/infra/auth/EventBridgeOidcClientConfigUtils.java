package com.redhat.service.bridge.infra.auth;

import io.quarkus.oidc.client.OidcClientConfig;

public class EventBridgeOidcClientConfigUtils {

    // TODO: review this logic since it should be provided by the quarkus module.
    public static OidcClientConfig.Grant.Type getGrantType(String type) {
        switch (type) {
            case "password":
                return OidcClientConfig.Grant.Type.PASSWORD;
            case "authorization_code":
                return OidcClientConfig.Grant.Type.CODE;
            case "urn:ietf:params:oauth:grant-type:token-exchange":
                return OidcClientConfig.Grant.Type.EXCHANGE;
            case "refresh_token":
                return OidcClientConfig.Grant.Type.REFRESH;
            default:
                throw new RuntimeException("Unrecognized OIDC grant type " + type);
        }
    }
}
