package com.redhat.service.bridge.rhoas.auth;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.Tokens;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

public class MasSSOHeaderFactory implements ClientHeadersFactory {

    @Inject
    @NamedOidcClient("mas-sso")
    Tokens tokens;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add("Authorization", "Bearer " + tokens.getAccessToken());
        return result;
    }

}
