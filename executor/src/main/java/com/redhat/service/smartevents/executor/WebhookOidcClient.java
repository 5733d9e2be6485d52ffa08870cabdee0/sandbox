package com.redhat.service.smartevents.executor;

import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.core.auth.AbstractOidcClient;
import com.redhat.service.smartevents.infra.core.auth.OidcClientConstants;

import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class WebhookOidcClient extends AbstractOidcClient {

    private static final String NAME = OidcClientConstants.WEBHOOK_OIDC_CLIENT_NAME;

    @ConfigProperty(name = "event-bridge.sso.auth-server-url")
    String serverUrl;

    @ConfigProperty(name = "event-bridge.sso.webhook-client-id")
    String clientId;

    @ConfigProperty(name = "event-bridge.sso.credentials.webhook-client-secret")
    String secret;

    public void init(@Observes StartupEvent event) {
        // force instantiation of bean - see https://github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox/pull/1196#pullrequestreview-1134203409
    }

    @Inject
    public WebhookOidcClient(OidcClients oidcClients, ScheduledExecutorService executorService) {
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
