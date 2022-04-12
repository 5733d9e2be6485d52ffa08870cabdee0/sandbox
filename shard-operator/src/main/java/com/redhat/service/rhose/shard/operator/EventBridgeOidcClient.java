package com.redhat.service.rhose.shard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.rhose.infra.auth.AbstractOidcClient;

import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class EventBridgeOidcClient extends AbstractOidcClient {

    private static final String NAME = "event-bridge-sso";

    @ConfigProperty(name = "event-bridge.sso.auth-server-url")
    String serverUrl;

    @ConfigProperty(name = "event-bridge.sso.client-id")
    String clientId;

    @ConfigProperty(name = "event-bridge.sso.credentials.secret")
    String secret;

    @Inject
    public EventBridgeOidcClient(OidcClients oidcClients) {
        super(NAME, oidcClients);
    }

    @Override
    protected OidcClientConfig getOidcClientConfig() {
        OidcClientConfig oidcClientConfig = new OidcClientConfig();
        oidcClientConfig.setId(NAME);
        oidcClientConfig.setAuthServerUrl(serverUrl);
        oidcClientConfig.setClientId(clientId);
        oidcClientConfig.getCredentials().setSecret(secret);
        oidcClientConfig.setRefreshTokenTimeSkew(AbstractOidcClient.REFRESH_TOKEN_TIME_SKEW);

        return oidcClientConfig;
    }

    @Override
    @Scheduled(every = AbstractOidcClient.SCHEDULER_TIME)
    protected void scheduledLoop() {
        super.checkAndRefresh();
    }
}
