package com.redhat.service.smartevents.rhoas;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.cloud.api.kas.models.ServiceAccount;
import com.openshift.cloud.api.kas.models.ServiceAccountRequest;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

public class KafkasMgmtV1ClientImpl extends AbstractAppServicesClientImpl implements KafkasMgmtV1Client {

    private static final Logger LOG = LoggerFactory.getLogger(KafkasMgmtV1ClientImpl.class);
    private static final Duration SSO_CONNECTION_TIMEOUT = Duration.ofSeconds(30);

    private final String basePath;
    private final String refreshToken;

    private final OidcClient client;

    Tokens tokens;

    public KafkasMgmtV1ClientImpl(Vertx vertx, String basePath, OidcClient client, String refreshToken) {
        super(vertx);
        this.basePath = basePath;
        this.client = client;
        this.refreshToken = refreshToken;
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
            try {
                tokens = client.refreshTokens(refreshToken).await().atMost(SSO_CONNECTION_TIMEOUT);
            } catch (RuntimeException e) {
                String msg = "Could not fetch a new authentication token from red-hat-sso server";
                LOG.warn(msg);
                throw new InternalPlatformException(msg, e);
            }
        }
        return tokens.getAccessToken();
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }
}
