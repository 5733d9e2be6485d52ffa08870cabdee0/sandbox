package com.redhat.service.bridge.shard.operator;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.bridge.infra.auth.AbstractOidcClient;

import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class EventBridgeOidcClient extends AbstractOidcClient {

    private static final String NAME = "event-bridge-sso";

    @ConfigProperty(name = "event-bridge.auth-server-url")
    String serverUrl;

    @ConfigProperty(name = "event-bridge.client-id")
    String clientId;

    @ConfigProperty(name = "event-bridge.credentials.secret")
    String secret;

    @ConfigProperty(name = "event-bridge.grant.type")
    String type;

    @ConfigProperty(name = "event-bridge.grant-options.password.username")
    String username;

    @ConfigProperty(name = "event-bridge.grant-options.password.password")
    String password;

    public EventBridgeOidcClient() {
        super(NAME);
    }

    @Inject
    public EventBridgeOidcClient(OidcClients clients) {
        super(NAME, clients);
    }

    @PostConstruct
    void init() {
        Map<String, Map<String, String>> grantOptions = new HashMap<>();
        Map<String, String> passwordConfig = new HashMap<>();
        passwordConfig.put("username", username);
        passwordConfig.put("password", password);
        grantOptions.put("password", passwordConfig);

        OidcClientConfig oidcClientConfig = new OidcClientConfig();
        oidcClientConfig.setId("event-bridge.sso");
        oidcClientConfig.setAuthServerUrl(serverUrl);
        oidcClientConfig.setClientId(clientId);
        oidcClientConfig.grant.setType(OidcClientConfig.Grant.Type.PASSWORD);
        oidcClientConfig.setGrantOptions(grantOptions);
        oidcClientConfig.getCredentials().setSecret(secret);
        oidcClientConfig.setRefreshTokenTimeSkew(AbstractOidcClient.REFRESH_TOKEN_TIME_SKEW);

        this.populate(oidcClientConfig);
    }

    @Scheduled(every = AbstractOidcClient.SCHEDULER_TIME)
    void refresh() {
        checkAndRefresh();
    }
}
