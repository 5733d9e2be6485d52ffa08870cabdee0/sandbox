package com.redhat.service.bridge.shard.operator.watchers.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;

public class KubernetesIngressEventSource extends AbstractEventSource implements Watcher<Ingress> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesIngressEventSource.class);

    private final KubernetesClient client;

    private final String component;

    public static KubernetesIngressEventSource createAndRegisterWatch(KubernetesClient client, String component) {
        KubernetesIngressEventSource eventSource = new KubernetesIngressEventSource(client, component);
        eventSource.registerWatch(component);
        return eventSource;
    }

    private KubernetesIngressEventSource(KubernetesClient client, String component) {
        this.client = client;
        this.component = component;
    }

    private void registerWatch(String component) {
        client.network().v1().ingresses().inAnyNamespace()
                .withLabels(
                        new LabelsBuilder()
                                .withManagedByOperator()
                                .withComponent(component)
                                .build())
                .watch(this);
    }

    @Override
    public void eventReceived(Action action, Ingress ingress) {
        if (eventHandler == null) {
            LOGGER.warn("Ignoring action {} for resource Ingress. EventHandler has not yet been initialized.", action);
            return;
        }

        LOGGER.info(
                "Event received for action: {}, {}: {}",
                action.name(),
                "Ingress",
                ingress.getMetadata().getName());

        if (action == Action.ERROR) {
            LOGGER.warn(
                    "Skipping {} event for {} uid: {}, version: {}",
                    action,
                    "Ingress",
                    ingress.getMetadata().getUid(),
                    ingress.getMetadata().getResourceVersion());
            return;
        }

        if (ingress.getMetadata().getOwnerReferences().isEmpty()) {
            LOGGER.warn("Unable to retrieve Owner UID. Ignoring event {} {}/{}", ingress.getMetadata().getNamespace(),
                    ingress.getKind(), ingress.getMetadata().getName());
            return;
        }

        eventHandler.handleEvent(new NetworkResourceEvent(action, ingress.getMetadata().getOwnerReferences().get(0), this));
    }

    @Override
    public void onClose(WatcherException e) {
        if (e == null) {
            return;
        }

        if (e.isHttpGone()) {
            LOGGER.warn("Received error for watch, will try to reconnect.", e);
            registerWatch(this.component);
        } else {
            // Note that this should not happen normally, since fabric8 client handles reconnect.
            // In case it tries to reconnect this method is not called.
            LOGGER.error("Unexpected error happened with watch. Will exit.", e);
            System.exit(1);
        }
    }
}
