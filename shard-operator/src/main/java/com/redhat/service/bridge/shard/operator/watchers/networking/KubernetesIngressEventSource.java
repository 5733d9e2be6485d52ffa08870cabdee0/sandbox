package com.redhat.service.bridge.shard.operator.watchers.networking;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;
import com.redhat.service.bridge.shard.operator.watchers.BaseEventSource;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesIngressEventSource extends BaseEventSource<Ingress, NetworkResourceEvent> {

    private final KubernetesClient client;

    public static KubernetesIngressEventSource createAndRegisterWatch(KubernetesClient client, String component) {
        KubernetesIngressEventSource eventSource = new KubernetesIngressEventSource(client, component);
        eventSource.registerWatch(component);
        return eventSource;
    }

    private KubernetesIngressEventSource(KubernetesClient client, String component) {
        super(component);
        this.client = client;
    }

    @Override
    protected NetworkResourceEvent newEvent(Action action, Ingress resource) {
        return new NetworkResourceEvent(action, resource.getMetadata().getOwnerReferences().get(0), this);
    }

    protected void registerWatch(String component) {
        client.network().v1().ingresses().inAnyNamespace()
                .withLabels(
                        new LabelsBuilder()
                                .withManagedByOperator()
                                .withComponent(component)
                                .build())
                .watch(this);
    }
}
