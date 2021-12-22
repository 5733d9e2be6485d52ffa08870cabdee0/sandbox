package com.redhat.service.bridge.rhoas;

import javax.annotation.PostConstruct;
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
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
public class KafkasMgmtV1ClientImpl extends AbstractAppServicesClientImpl implements KafkasMgmtV1Client {

    private static final Logger LOG = LoggerFactory.getLogger(KafkasMgmtV1ClientImpl.class);

    @ConfigProperty(name = "event-bridge.rhoas.mgmt-api.host")
    String basePath;
    @ConfigProperty(name = "event-bridge.rhoas.sso.red-hat.refresh-token")
    String refreshToken;

    @Inject
    Vertx vertx;

    @Inject
    @NamedOidcClient("red-hat-sso")
    OidcClient client;

    Tokens tokens;

    @PostConstruct
    void onPostConstruct() {
        init(vertx);
    }

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
            tokens = client.refreshTokens(refreshToken).await().indefinitely();
        }
        return tokens.getAccessToken();
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }
}
