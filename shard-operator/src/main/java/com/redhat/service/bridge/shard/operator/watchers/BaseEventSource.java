package com.redhat.service.bridge.shard.operator.watchers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;
import io.quarkus.runtime.Quarkus;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;

public abstract class BaseEventSource<R extends HasMetadata, E extends DefaultEvent> extends AbstractEventSource implements Watcher<R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentEventSource.class);

    private final String component;

    public BaseEventSource(final String component) {
        this.component = component;
    }

    protected abstract E newEvent(Watcher.Action action, R resource);

    protected abstract void registerWatch(String component);

    @Override
    public void eventReceived(Action action, R resource) {
        if (eventHandler == null) {
            LOGGER.warn("Ignoring action '{}' for resource '{}'. EventHandler has not yet been initialized.", resource.getKind(), action);
            return;
        }

        LOGGER.info(
                "Event received for action: '{}', '{}': '{}'",
                action.name(),
                resource.getKind(),
                resource.getMetadata().getName());

        if (action == Action.ERROR) {
            LOGGER.warn(
                    "Skipping '{}' event for custom resource uid: '{}', version: '{}'",
                    action,
                    getUID(resource),
                    getVersion(resource));
            return;
        }

        if (resource.getMetadata().getOwnerReferences().isEmpty()) {
            LOGGER.warn("Unable to retrieve Owner UID. Ignoring event {} {}/{}", resource.getMetadata().getNamespace(),
                    resource.getKind(), resource.getMetadata().getName());
            return;
        }

        eventHandler.handleEvent(this.newEvent(action, resource));
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
