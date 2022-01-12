package com.redhat.service.bridge.rhoas;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;

@Dependent
public class RhoasProducer {

    @Produces
    @ApplicationScoped
    @IfBuildProperty(name = RhoasClient.ENABLED_FLAG, stringValue = "true")
    public RhoasClient produceRealRhoasClient(KafkasMgmtV1Client mgmtClient, KafkaInstanceAdminClient instanceClient) {
        return new RhoasClientImpl(mgmtClient, instanceClient);
    }

    @Produces
    @DefaultBean
    public RhoasClient produceNoopRhoasClient() {
        return null;
    }
}