package com.redhat.service.bridge.rhoas.auth;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

public class RedHatSSOHeaderFactory implements ClientHeadersFactory {

    @ConfigProperty(name = "RED_HAT_SSO_REFRESH_TOKEN")
    String refreshToken;

    @Inject
    @NamedOidcClient("red-hat-sso")
    OidcClient client;

    Tokens tokens;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        if (tokens == null || tokens.isAccessTokenExpired()) {
            tokens = client.refreshTokens(refreshToken).await().indefinitely();
        }

        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add("Authorization", "Bearer " + tokens.getAccessToken());
        return result;
    }

}
