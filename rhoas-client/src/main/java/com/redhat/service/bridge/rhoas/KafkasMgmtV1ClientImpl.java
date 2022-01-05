package com.redhat.service.bridge.rhoas;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.cloud.api.kas.models.ServiceAccount;
import com.openshift.cloud.api.kas.models.ServiceAccountRequest;

import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class KafkasMgmtV1ClientImpl extends AbstractAppServicesClientImpl implements KafkasMgmtV1Client {

    private static final Logger LOG = LoggerFactory.getLogger(KafkasMgmtV1ClientImpl.class);
    private static final Duration SSO_CONNECTION_TIMEOUT = Duration.ofSeconds(30);

    @ConfigProperty(name = "event-bridge.rhoas.mgmt-api.host")
    String basePath;
    @ConfigProperty(name = "event-bridge.rhoas.sso.red-hat.refresh-token")
    String refreshToken;

    @Inject
    @NamedOidcClient("red-hat-sso")
    OidcClient client;

    Tokens tokens;

    @Override
    public Uni<ServiceAccount> createServiceAccount(ServiceAccountRequest serviceAccountRequest) {
        return securityValueCall(api -> api.createServiceAccount(serviceAccountRequest));
    }

    @Override
    public Uni<Void> deleteServiceAccount(String id) {
        return securityVoidCall(api -> api.deleteServiceAccountById(id));
    }

    @Override
    protected String getAccessToken() {
        if (tokens == null || tokens.isAccessTokenExpired()) {
            try {
                tokens = client.refreshTokens(refreshToken).await().atMost(SSO_CONNECTION_TIMEOUT);
            } catch (RuntimeException e) {
                LOG.warn("Could not fetch a new authentication token from red-hat-sso server");
                throw e;
            }
        }
        return tokens.getAccessToken();
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }
}
