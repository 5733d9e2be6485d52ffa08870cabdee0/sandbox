package com.redhat.service.smartevents.infra.core.auth;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;

public abstract class AbstractOidcClient implements com.redhat.service.smartevents.infra.core.auth.OidcClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOidcClient.class);

    protected static final String SCHEDULER_TIME = "5s";
    protected static final Duration SSO_CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    protected static final Duration REFRESH_TOKEN_TIME_SKEW = Duration.ofSeconds(30);

    protected String name;
    protected OidcClient client;
    private OidcClients oidcClients;
    protected Duration timeout;
    protected Tokens currentTokens;
    protected ScheduledExecutorService executorService;

    protected AbstractOidcClient() {
    }

    protected AbstractOidcClient(String name, OidcClients oidcClients, Duration timeout, ScheduledExecutorService executorService) {
        this.name = name;
        this.oidcClients = oidcClients;
        this.timeout = timeout;
        this.executorService = executorService;
    }

    protected AbstractOidcClient(String name, OidcClients oidcClients, ScheduledExecutorService executorService) {
        this(name, oidcClients, SSO_CONNECTION_TIMEOUT, executorService);
    }

    protected abstract OidcClientConfig getOidcClientConfig();

    @PostConstruct
    protected void init() {
        this.client = oidcClients.newClient(getOidcClientConfig()).await().atMost(timeout);
        retrieveTokens();
        this.executorService.scheduleWithFixedDelay(() -> {
            try {
                checkAndRefresh();
            } catch (Exception e) {
                LOGGER.warn("OidcClient '{}' raised an exception during the checkAndRefresh procedure.", this.name, e);
            }
        }, timeout.toSeconds(), timeout.toSeconds(), TimeUnit.SECONDS);
    }

    @PreDestroy
    protected void destroy() {
        if (this.executorService != null && !this.executorService.isShutdown()) {
            this.executorService.shutdownNow();
        }
    }

    protected void checkAndRefresh() {
        if (currentTokens.isAccessTokenExpired() || currentTokens.isAccessTokenWithinRefreshInterval()) {
            refreshTokens();
            LOGGER.info("Tokens have been refreshed for OIDC client '{}'", name);
        }
    }

    public String getToken() {
        if (currentTokens == null) {
            throw getOidcTokensNotInitializedException(String.format("Tokens for OIDC client '%s' are not initialized.", name));
        }
        return currentTokens.getAccessToken();
    }

    protected abstract InternalPlatformException getOidcTokensNotInitializedException(String message);

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

    protected void retrieveTokens() {
        currentTokens = client.getTokens().await().atMost(timeout);
        LOGGER.info("New token for OIDC client '{}' has been set", name);
    }
}
