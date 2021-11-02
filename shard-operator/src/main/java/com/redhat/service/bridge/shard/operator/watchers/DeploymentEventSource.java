package com.redhat.service.bridge.shard.operator.watchers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;

/**
 * Used by the controllers to watch changes on Deployment objects.
 *
 */
public class DeploymentEventSource extends AbstractEventSource implements Watcher<Deployment> {
    private static final Logger log = LoggerFactory.getLogger(DeploymentEventSource.class);

    private final KubernetesClient client;

    private final String applicationType;

    public static DeploymentEventSource createAndRegisterWatch(KubernetesClient client, String applicationType) {
        DeploymentEventSource deploymentEventSource = new DeploymentEventSource(client, applicationType);
        deploymentEventSource.registerWatch(applicationType);
        return deploymentEventSource;
    }

    private DeploymentEventSource(KubernetesClient client, String applicationType) {
        this.client = client;
        this.applicationType = applicationType;
    }

    private void registerWatch(String applicationType) {
        client
                .apps()
                .deployments()
                .inAnyNamespace()
                .withLabel(LabelsBuilder.MANAGED_BY_LABEL, LabelsBuilder.OPERATOR_NAME)
                .withLabel(LabelsBuilder.APPLICATION_TYPE_LABEL, applicationType)
                .watch(this);
    }

    @Override
    public void eventReceived(Action action, Deployment deployment) {
        log.info(
                "Event received for action: {}, Deployment: {}",
                action.name(),
                deployment.getMetadata().getName());

        if (action == Action.ERROR) {
            log.warn(
                    "Skipping {} event for custom resource uid: {}, version: {}",
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
            return;
        }
        if (e.isHttpGone()) {
            log.warn("Received error for watch, will try to reconnect.", e);
            registerWatch(this.applicationType);
        } else {
            // Note that this should not happen normally, since fabric8 client handles reconnect.
            // In case it tries to reconnect this method is not called.
            log.error("Unexpected error happened with watch. Will exit.", e);
            System.exit(1);
        }
    }
}
