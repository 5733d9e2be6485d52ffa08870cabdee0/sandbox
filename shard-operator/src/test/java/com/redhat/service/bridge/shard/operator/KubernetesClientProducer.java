package com.redhat.service.bridge.shard.operator;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

@ApplicationScoped
public class KubernetesClientProducer {

    private final KubernetesServer mockServer;

    private final KubernetesClient client;

    public KubernetesClientProducer() {
        List<CustomResourceDefinitionContext> crds = new ArrayList<>();
        this.mockServer = new KubernetesServer(false, true, crds);
        this.mockServer.before();
        this.client = mockServer.getClient();
    }

    @Produces
    KubernetesServer getMockServer() {
        return mockServer;
    }

    @Produces
    KubernetesClient getClient() {
        return client;
    }
}