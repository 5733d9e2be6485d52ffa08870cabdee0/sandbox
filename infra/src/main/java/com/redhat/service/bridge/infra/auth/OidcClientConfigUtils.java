package com.redhat.service.bridge.infra.auth;

import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;

public class OidcClientConfigUtils {

    public static OidcClientConfig.Grant.Type getGrantType(String type) {
        switch (type) {
            case OidcConstants.PASSWORD_GRANT:
                return OidcClientConfig.Grant.Type.PASSWORD;
            case OidcConstants.AUTHORIZATION_CODE:
                return OidcClientConfig.Grant.Type.CODE;
            case OidcConstants.EXCHANGE_GRANT:
                return OidcClientConfig.Grant.Type.EXCHANGE;
            case OidcConstants.REFRESH_TOKEN_GRANT:
                return OidcClientConfig.Grant.Type.REFRESH;
            default:
                throw new RuntimeException("Unrecognized OIDC grant type " + type);
        }
    }
}
