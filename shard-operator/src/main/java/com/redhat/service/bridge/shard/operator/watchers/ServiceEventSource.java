package com.redhat.service.bridge.shard.operator.watchers;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;

public class ServiceEventSource extends BaseEventSource<Service, ServiceEvent> {

    private final KubernetesClient client;

    public static ServiceEventSource createAndRegisterWatch(KubernetesClient client, String component) {
        ServiceEventSource serviceEventSource = new ServiceEventSource(client, component);
        serviceEventSource.registerWatch(component);
        return serviceEventSource;
    }

    private ServiceEventSource(KubernetesClient client, String component) {
        super(component);
        this.client = client;
    }

    @Override
    protected ServiceEvent newEvent(Action action, Service resource) {
        return new ServiceEvent(action, resource, this);
    }

    @Override
    protected void registerWatch(String component) {
        client
                .services()
                .inAnyNamespace()
                .withLabels(
                        new LabelsBuilder()
                                .withManagedByOperator()
                                .withComponent(component)
                                .build())
                .watch(this);
    }
}
