package com.redhat.service.rhose.rhoas;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.client.OidcClients;
import io.vertx.mutiny.core.Vertx;

@Dependent
public class RhoasProducer {

    private static final String MAS_SSO_CLIENT = "mas-sso";

    private static final String REDHAT_SSO_CLIENT = "red-hat-sso";

    @ConfigProperty(name = RhoasProperties.INSTANCE_API_HOST)
    String instanceApiHost;

    @ConfigProperty(name = RhoasProperties.MGMT_API_HOST)
    String mgmtApiHost;

    @ConfigProperty(name = RhoasProperties.SSO_RED_HAT_REFRESH_TOKEN)
    String refreshToken;

    @Inject
    Vertx vertx;

    @Inject
    OidcClients oidcClients;

    @Produces
    @ApplicationScoped
    public RhoasClient produceRhoasClient(KafkasMgmtV1Client mgmtClient, KafkaInstanceAdminClient instanceClient) {
        return new RhoasClientImpl(mgmtClient, instanceClient);
    }

    @Produces
    @ApplicationScoped
    public KafkaInstanceAdminClient produceKafkaInstanceAdminClientImpl() {
        return new KafkaInstanceAdminClientImpl(vertx, instanceApiHost, oidcClients.getClient(MAS_SSO_CLIENT));
    }

    @Produces
    @ApplicationScoped
    public KafkasMgmtV1Client produceKafkasMgmtV1Client() {
        return new KafkasMgmtV1ClientImpl(vertx, mgmtApiHost, oidcClients.getClient(REDHAT_SSO_CLIENT), refreshToken);
    }
}
