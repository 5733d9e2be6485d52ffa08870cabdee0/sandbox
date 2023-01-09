package com.redhat.service.smartevents.shard.operator.v1.auth;

import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.core.auth.AbstractOidcClient;
import com.redhat.service.smartevents.infra.v1.api.auth.AbstractOidcClientV1;

import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;

@ApplicationScoped
public class EventBridgeOidcClient extends AbstractOidcClientV1 {

    private static final String NAME = "event-bridge-sso";

    @ConfigProperty(name = "event-bridge.sso.auth-server-url")
    String serverUrl;

    @ConfigProperty(name = "event-bridge.sso.client-id")
    String clientId;

    @ConfigProperty(name = "event-bridge.sso.credentials.secret")
    String secret;

    @Inject
    public EventBridgeOidcClient(OidcClients oidcClients, ScheduledExecutorService executorService) {
        super(NAME, oidcClients, executorService);
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
}
