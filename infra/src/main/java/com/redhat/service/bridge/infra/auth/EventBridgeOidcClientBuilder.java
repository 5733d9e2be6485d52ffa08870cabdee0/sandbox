package com.redhat.service.bridge.infra.auth;

import java.time.Duration;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;

public class EventBridgeOidcClientBuilder {

    private String name;
    private OidcClients clients;
    private OidcClientConfig oidcClientConfig;
    private Duration timeout = EventBridgeOidcClientConstants.SSO_CONNECTION_TIMEOUT;

    public EventBridgeOidcClientBuilder() {
    }

    public EventBridgeOidcClientBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public EventBridgeOidcClientBuilder withOidcClients(OidcClients oidcClients) {
        this.clients = oidcClients;
        return this;
    }

    public EventBridgeOidcClientBuilder withOidcClientConfig(OidcClientConfig oidcClientConfig) {
        this.oidcClientConfig = oidcClientConfig;
        return this;
    }

    public EventBridgeOidcClientBuilder withTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public EventBridgeOidcClient build() {
        OidcClient client = clients.newClient(oidcClientConfig).await().atMost(timeout);
        return new EventBridgeOidcClient(name, client, timeout);
    }
}
