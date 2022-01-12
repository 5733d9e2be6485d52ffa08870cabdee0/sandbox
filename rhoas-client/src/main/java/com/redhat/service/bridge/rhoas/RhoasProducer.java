package com.redhat.service.bridge.rhoas;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.oidc.client.OidcClients;
import io.vertx.mutiny.core.Vertx;

@Dependent
public class RhoasProducer {

    @Produces
    @ApplicationScoped
    @IfBuildProperty(name = RhoasClient.ENABLED_FLAG, stringValue = "true")
    public RhoasClient produceRhoasClient(KafkasMgmtV1Client mgmtClient, KafkaInstanceAdminClient instanceClient) {
        return new RhoasClientImpl(mgmtClient, instanceClient);
    }

    @Produces
    @ApplicationScoped
    @IfBuildProperty(name = RhoasClient.ENABLED_FLAG, stringValue = "true")
    public KafkaInstanceAdminClient produceKafkaInstanceAdminClientImpl(Vertx vertx, OidcClients oidcClients) {
        String basePath = ConfigProvider.getConfig().getValue("event-bridge.rhoas.instance-api.host", String.class);
        return new KafkaInstanceAdminClientImpl(vertx, basePath, oidcClients.getClient("mas-sso"));
    }

    @Produces
    @ApplicationScoped
    @IfBuildProperty(name = RhoasClient.ENABLED_FLAG, stringValue = "true")
    public KafkasMgmtV1Client produceKafkasMgmtV1Client(Vertx vertx, OidcClients oidcClients) {
        String basePath = ConfigProvider.getConfig().getValue("event-bridge.rhoas.mgmt-api.host", String.class);
        String refreshToken = ConfigProvider.getConfig().getValue("event-bridge.rhoas.sso.red-hat.refresh-token", String.class);
        return new KafkasMgmtV1ClientImpl(vertx, basePath, oidcClients.getClient("red-hat-sso"), refreshToken);
    }
}
