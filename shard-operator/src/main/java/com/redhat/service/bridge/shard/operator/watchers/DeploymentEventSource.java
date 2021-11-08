package com.redhat.service.bridge.shard.operator.watchers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.quarkus.runtime.Quarkus;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;

/**
 * Used by the controllers to watch changes on Deployment objects.
 *
 */
public class DeploymentEventSource extends AbstractEventSource implements Watcher<Deployment> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentEventSource.class);

    private final KubernetesClient client;

    private final String component;

    public static DeploymentEventSource createAndRegisterWatch(KubernetesClient client, String component) {
        DeploymentEventSource deploymentEventSource = new DeploymentEventSource(client, component);
        deploymentEventSource.registerWatch(component);
        return deploymentEventSource;
    }

    private DeploymentEventSource(KubernetesClient client, String component) {
        this.client = client;
        this.component = component;
    }

    private void registerWatch(String component) {
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

    @Override
    public void eventReceived(Action action, Deployment deployment) {
        LOGGER.info(
                "Event received for action: '{}', Deployment: '{}'",
                action.name(),
                deployment.getMetadata().getName());

        if (action == Action.ERROR) {
            LOGGER.warn(
                    "Skipping '{}' event for custom resource uid: '{}', version: '{}'",
                    action,
                    getUID(deployment),
                    getVersion(deployment));
            return;
        }

        eventHandler.handleEvent(new DeploymentEvent(action, deployment, this));
    }

    @Override
    public void onClose(WatcherException e) {
        if (e == null) {
            LOGGER.warn("Unknown error happened with watch.");
            return;
        }
        if (e.isHttpGone()) {
            LOGGER.warn("Received error for watch, will try to reconnect.", e);
            registerWatch(this.component);
        } else {
            // Note that this should not happen normally, since fabric8 client handles reconnect.
            // In case it tries to reconnect this method is not called.
            LOGGER.error("Unexpected error happened with watch. Will exit.", e);
            Quarkus.asyncExit(1);
        }
    }
}
