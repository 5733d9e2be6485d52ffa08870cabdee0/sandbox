package com.redhat.service.bridge.infra.auth;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.exceptions.definitions.platform.OidcTokensNotInitializedException;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;

public abstract class AbstractOidcClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOidcClient.class);

    protected static final String SCHEDULER_TIME = "5s";
    protected static final Duration SSO_CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    protected static final Duration REFRESH_TOKEN_TIME_SKEW = Duration.ofSeconds(30);

    private String name;
    private OidcClient client;
    private OidcClients oidcClients;
    private Duration timeout;
    private Tokens currentTokens;

    public AbstractOidcClient() {
    }

    public AbstractOidcClient(String name, OidcClients oidcClients, Duration timeout) {
        this.name = name;
        this.oidcClients = oidcClients;
        this.timeout = timeout;
    }

    public AbstractOidcClient(String name, OidcClients oidcClients) {
        this(name, oidcClients, SSO_CONNECTION_TIMEOUT);
    }

    protected abstract OidcClientConfig getOidcClientConfig();

    protected abstract void scheduledLoop();

    @PostConstruct
    protected void init() {
        this.client = oidcClients.newClient(getOidcClientConfig()).await().atMost(timeout);
        retrieveTokens();
    }

    protected void checkAndRefresh() {
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

    public String getName() {
        return this.name;
    }

    private void refreshTokens() {
        Tokens tokens = currentTokens;
        try {
            currentTokens = client.refreshTokens(tokens.getRefreshToken()).await().atMost(timeout);
        } catch (OidcClientException e) {
            LOGGER.warn("Could not use refresh token. Trying to get a new fresh token for OIDC client '{}'", name);
            retrieveTokens();
        }
    }

    private void retrieveTokens() {
        currentTokens = client.getTokens().await().atMost(timeout);
        LOGGER.info("New token for OIDC client '{}' has been set", name);
    }
}
