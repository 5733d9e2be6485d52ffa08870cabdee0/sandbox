package com.redhat.service.rhose.manager.connectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.rhose.infra.auth.AbstractOidcClient;
import com.redhat.service.rhose.infra.auth.OidcClientConfigUtils;

import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class ConnectorsOidcClient extends AbstractOidcClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsOidcClient.class);

    private static final String NAME = "managed-connectors-client";

    @ConfigProperty(name = "managed-connectors.auth.server-url")
    String authServerUrl;

    @ConfigProperty(name = "managed-connectors.auth.client-id")
    String clientId;

    @ConfigProperty(name = "managed-connectors.auth.offline-token")
    String offlineToken;

    @Inject
    public ConnectorsOidcClient(OidcClients oidcClients) {
        super(NAME, oidcClients);
    }

    @Override
    protected OidcClientConfig getOidcClientConfig() {

        OidcClientConfig oidcClientConfig = new OidcClientConfig();
        oidcClientConfig.setId(NAME);
        oidcClientConfig.setAuthServerUrl(authServerUrl);
        oidcClientConfig.setClientId(clientId);
        oidcClientConfig.grant.setType(OidcClientConfigUtils.getGrantType(OidcConstants.REFRESH_TOKEN_GRANT));
        oidcClientConfig.getCredentials().setSecret("secret");
        oidcClientConfig.setRefreshTokenTimeSkew(AbstractOidcClient.REFRESH_TOKEN_TIME_SKEW);

        return oidcClientConfig;
    }

    @Override
    @Scheduled(every = AbstractOidcClient.SCHEDULER_TIME)
    protected void scheduledLoop() {
        super.checkAndRefresh();
    }

    @Override
    protected void retrieveTokens() {
        currentTokens = client.refreshTokens(offlineToken).await().atMost(timeout);
        LOGGER.info("New token for OIDC client '{}' has been set", name);
    }
}
