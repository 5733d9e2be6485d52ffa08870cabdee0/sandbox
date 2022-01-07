package com.redhat.service.bridge.infra.auth;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.exceptions.definitions.platform.OidcTokensNotInitializedException;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.Tokens;

public class EventBridgeOidcClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBridgeOidcClient.class);

    private String name;
    private OidcClient client;
    private Duration timeout;
    private Tokens currentTokens;

    public EventBridgeOidcClient(String name, OidcClient oidcClient, Duration timeout) {
        this.name = name;
        this.client = oidcClient;
        this.timeout = timeout;
    }

    public void checkAndRefresh() {
        if (currentTokens.isAccessTokenExpired() || currentTokens.isAccessTokenWithinRefreshInterval()) {
            refreshTokens();
            LOGGER.info("Tokens have been refreshed for OIDC client '{}'", name);
        }
    }

    public String getToken() {
        if (currentTokens == null) {
            throw new OidcTokensNotInitializedException(String.format("Tokens for OIDC client '%s' are not initialized.", name));
        }
        return currentTokens.getAccessToken();
    }

    private void refreshTokens() {
        Tokens tokens = currentTokens;
        try {
            currentTokens = client.refreshTokens(tokens.getRefreshToken()).await().atMost(timeout);
        } catch (OidcClientException e) {
            LOGGER.warn("Could not use refresh token. Trying to get a new fresh token for OIDC client '{}'", name);
            init();
        }
    }

    public void init() {
        currentTokens = client.getTokens().await().atMost(timeout);
        LOGGER.info("New token for OIDC client '{}' has been set", name);
    }
}
