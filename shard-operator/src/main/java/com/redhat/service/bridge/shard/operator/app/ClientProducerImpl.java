package com.redhat.service.bridge.shard.operator.app;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

@Singleton
public class ClientProducerImpl implements ClientProducer {

    private final OpenShiftClient client = new DefaultOpenShiftClient();

    @Override
    @Produces
    public OpenShiftClient produceClient() {
        return client;
    }
}