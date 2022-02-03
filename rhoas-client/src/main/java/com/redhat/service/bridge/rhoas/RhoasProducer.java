package com.redhat.service.bridge.rhoas;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.oidc.client.OidcClients;
import io.vertx.mutiny.core.Vertx;

import static com.redhat.service.bridge.rhoas.RhoasProperties.INSTANCE_API_HOST;
import static com.redhat.service.bridge.rhoas.RhoasProperties.MGMT_API_HOST;
import static com.redhat.service.bridge.rhoas.RhoasProperties.SSO_RED_HAT_REFRESH_TOKEN;

@Dependent
public class RhoasProducer {

    @Produces
    @ApplicationScoped
    public RhoasClient produceRhoasClient(KafkasMgmtV1Client mgmtClient, KafkaInstanceAdminClient instanceClient) {
        return new RhoasClientImpl(mgmtClient, instanceClient);
    }

    @Produces
    @ApplicationScoped
    public KafkaInstanceAdminClient produceKafkaInstanceAdminClientImpl(Vertx vertx, OidcClients oidcClients) {
        String basePath = ConfigProvider.getConfig().getValue(INSTANCE_API_HOST, String.class);
        return new KafkaInstanceAdminClientImpl(vertx, basePath, oidcClients.getClient("mas-sso"));
    }

    @Produces
    @ApplicationScoped
    public KafkasMgmtV1Client produceKafkasMgmtV1Client(Vertx vertx, OidcClients oidcClients) {
        String basePath = ConfigProvider.getConfig().getValue(MGMT_API_HOST, String.class);
        String refreshToken = ConfigProvider.getConfig().getValue(SSO_RED_HAT_REFRESH_TOKEN, String.class);
        return new KafkasMgmtV1ClientImpl(vertx, basePath, oidcClients.getClient("red-hat-sso"), refreshToken);
    }
}
