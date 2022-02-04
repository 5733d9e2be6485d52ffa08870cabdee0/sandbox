package com.redhat.service.bridge.shard.operator.watchers;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Used by the controllers to watch changes on Deployment objects.
 */
public class DeploymentEventSource extends BaseEventSource<Deployment, DeploymentEvent> {

    private final KubernetesClient client;

    public static DeploymentEventSource createAndRegisterWatch(KubernetesClient client, String component) {
        DeploymentEventSource deploymentEventSource = new DeploymentEventSource(client, component);
        deploymentEventSource.registerWatch(component);
        return deploymentEventSource;
    }

    private DeploymentEventSource(KubernetesClient client, String component) {
        super(component);
        this.client = client;
    }

    @Override
    protected DeploymentEvent newEvent(Action action, Deployment resource) {
        return new DeploymentEvent(action, resource, this);
    }

    @Override
    protected void registerWatch(String component) {
        client
                .apps()
                .deployments()
                .inAnyNamespace()
                .withLabels(
                        new LabelsBuilder()
                                .withManagedByOperator()
                                .withComponent(component)
                                .build())
                .watch(this);
    }
}
