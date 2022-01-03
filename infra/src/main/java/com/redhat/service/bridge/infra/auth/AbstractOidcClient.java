package com.redhat.service.bridge.infra.auth;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;

public abstract class AbstractOidcClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOidcClient.class);

    public static final String SCHEDULER_TIME = "5s";
    public static final Duration SSO_CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration REFRESH_TOKEN_TIME_SKEW = Duration.ofSeconds(30);

    private final String name;

    OidcClients clients;

    OidcClient client;

    Tokens currentTokens;

    public AbstractOidcClient(String name) {
        this.name = name;
    }

    public AbstractOidcClient(String name, OidcClients clients) {
        this(name);
        this.clients = clients;
    }

    public void populate(OidcClientConfig oidcClientConfig) {
        client = clients.newClient(oidcClientConfig).await().atMost(Duration.ofSeconds(5));
        retrieveAndSetNewToken();
    }

    public void checkAndRefresh() {
        if (currentTokens.isAccessTokenExpired() || currentTokens.isAccessTokenWithinRefreshInterval()) {
            refreshTokens();
            LOGGER.info("Tokens have been refreshed.");
        }
    }

    public String getToken() {
        return currentTokens.getAccessToken();
    }

    private void refreshTokens() {
        Tokens tokens = currentTokens;
        try {
            currentTokens = client.refreshTokens(tokens.getRefreshToken()).await().atMost(SSO_CONNECTION_TIMEOUT);
        } catch (OidcClientException e) {
            LOGGER.warn("Could not use refresh token. Trying to get a new fresh token.", e);
            retrieveAndSetNewToken();
        }
    }

    private void retrieveAndSetNewToken() {
        LOGGER.info("Setting new token");
        currentTokens = client.getTokens().await().atMost(SSO_CONNECTION_TIMEOUT);
    }
}
