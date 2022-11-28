package com.redhat.service.smartevents.manager.v1.connectors;

import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.auth.AbstractOidcClient;

import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;

@ApplicationScoped
public class ConnectorsOidcClient extends AbstractOidcClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsOidcClient.class);

    private static final String NAME = "managed-connectors-client";

    @ConfigProperty(name = "managed-connectors.auth.server-url")
    String authServerUrl;

    @ConfigProperty(name = "managed-connectors.auth.credentials.client-id")
    String clientId;

    @ConfigProperty(name = "managed-connectors.auth.credentials.secret")
    String secret;

    @Inject
    public ConnectorsOidcClient(OidcClients oidcClients, ScheduledExecutorService executorService) {
        super(NAME, oidcClients, executorService);
    }

    @Override
    protected OidcClientConfig getOidcClientConfig() {
        OidcClientConfig oidcClientConfig = new OidcClientConfig();
        oidcClientConfig.setId(NAME);
        oidcClientConfig.setAuthServerUrl(authServerUrl);
        oidcClientConfig.setClientId(clientId);
        oidcClientConfig.getCredentials().setSecret(secret);
        oidcClientConfig.setRefreshTokenTimeSkew(AbstractOidcClient.REFRESH_TOKEN_TIME_SKEW);

        return oidcClientConfig;
    }
}
