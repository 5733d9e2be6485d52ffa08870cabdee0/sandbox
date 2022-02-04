package com.redhat.service.bridge.shard.operator.watchers;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Used by the controllers to watch changes on Deployment objects.
 */
public class SecretEventSource extends BaseEventSource<Secret, SecretEvent> {

    private final KubernetesClient client;

    public static SecretEventSource createAndRegisterWatch(KubernetesClient client, String component) {
        SecretEventSource deploymentEventSource = new SecretEventSource(client, component);
        deploymentEventSource.registerWatch(component);
        return deploymentEventSource;
    }

    private SecretEventSource(KubernetesClient client, String component) {
        super(component);
        this.client = client;
    }

    @Override
    protected SecretEvent newEvent(Action action, Secret resource) {
        return new SecretEvent(action, resource, this);
    }

    @Override
    protected void registerWatch(String component) {
        client
                .secrets()
                .inAnyNamespace()
                .withLabels(
                        new LabelsBuilder()
                                .withManagedByOperator()
                                .withComponent(component)
                                .build())
                .watch(this);
    }
}
