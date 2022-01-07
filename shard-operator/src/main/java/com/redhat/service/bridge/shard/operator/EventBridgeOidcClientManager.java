package com.redhat.service.bridge.shard.operator;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.auth.EventBridgeOidcClient;
import com.redhat.service.bridge.infra.auth.EventBridgeOidcClientBuilder;
import com.redhat.service.bridge.infra.auth.EventBridgeOidcClientConfigUtils;
import com.redhat.service.bridge.infra.auth.EventBridgeOidcClientConstants;

import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.Quarkus;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class EventBridgeOidcClientManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBridgeOidcClientManager.class);
    private static final String NAME = "event-bridge-sso";

    private EventBridgeOidcClient eventBridgeOidcClient;

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

    @Inject
    OidcClients oidcClients;

    public EventBridgeOidcClientManager() {
    }

    @PostConstruct
    void init() {
        Map<String, Map<String, String>> grantOptions = new HashMap<>();
        Map<String, String> passwordConfig = new HashMap<>();
        passwordConfig.put(OidcConstants.PASSWORD_GRANT_USERNAME, username);
        passwordConfig.put(OidcConstants.PASSWORD_GRANT_PASSWORD, password);
        grantOptions.put(OidcConstants.PASSWORD_GRANT, passwordConfig);

        OidcClientConfig oidcClientConfig = new OidcClientConfig();
        oidcClientConfig.setId(NAME);
        oidcClientConfig.setAuthServerUrl(serverUrl);
        oidcClientConfig.setClientId(clientId);
        oidcClientConfig.grant.setType(EventBridgeOidcClientConfigUtils.getGrantType(type));
        oidcClientConfig.setGrantOptions(grantOptions);
        oidcClientConfig.getCredentials().setSecret(secret);
        oidcClientConfig.setRefreshTokenTimeSkew(EventBridgeOidcClientConstants.REFRESH_TOKEN_TIME_SKEW);

        try {
            this.eventBridgeOidcClient = new EventBridgeOidcClientBuilder()
                    .withOidcClients(oidcClients)
                    .withName(NAME)
                    .withOidcClientConfig(oidcClientConfig)
                    .build();
            this.eventBridgeOidcClient.init();
        } catch (RuntimeException e) {
            LOGGER.error(String.format("Could not initialize OIDC client '%s'. The application is going to be stopped.", NAME), e);
            Quarkus.asyncExit(1);
        }
    }

    @Scheduled(every = EventBridgeOidcClientConstants.SCHEDULER_TIME)
    void refresh() {
        eventBridgeOidcClient.checkAndRefresh();
    }

    public String getToken() {
        return eventBridgeOidcClient.getToken();
    }
}
