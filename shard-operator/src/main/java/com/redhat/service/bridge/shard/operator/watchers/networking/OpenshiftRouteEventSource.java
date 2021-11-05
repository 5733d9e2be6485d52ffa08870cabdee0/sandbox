package com.redhat.service.bridge.shard.operator.watchers.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;

public class OpenshiftRouteEventSource extends AbstractEventSource implements Watcher<Route> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenshiftRouteEventSource.class);

    private final OpenShiftClient client;

    private final String component;

    public static OpenshiftRouteEventSource createAndRegisterWatch(OpenShiftClient client, String component) {
        OpenshiftRouteEventSource eventSource = new OpenshiftRouteEventSource(client, component);
        eventSource.registerWatch(component);
        return eventSource;
    }

    private OpenshiftRouteEventSource(OpenShiftClient client, String component) {
        this.client = client;
        this.component = component;
    }

    private void registerWatch(String component) {
        client.routes().inAnyNamespace()
                .withLabels(
                        new LabelsBuilder()
                                .withManagedByOperator()
                                .withComponent(component)
                                .build())
                .watch(this);
    }

    @Override
    public void eventReceived(Action action, Route route) {
        if (eventHandler == null) {
            LOGGER.warn("Ignoring action {} for resource ingress. EventHandler has not yet been initialized.", action);
            return;
        }

        LOGGER.info(
                "Event received for action: {}, {}: {}",
                action.name(),
                "Ingress",
                route.getMetadata().getName());

        if (action == Action.ERROR) {
            LOGGER.warn(
                    "Skipping {} event for {} uid: {}, version: {}",
                    action,
                    "Route",
                    route.getMetadata().getUid(),
                    route.getMetadata().getResourceVersion());
            return;
        }

        if (route.getMetadata().getOwnerReferences().isEmpty()) {
            LOGGER.warn("Unable to retrieve Owner UID. Ignoring event {} {}/{}", route.getMetadata().getNamespace(),
                    route.getKind(), route.getMetadata().getName());
            return;
        }

        eventHandler.handleEvent(new NetworkResourceEvent(action, route.getMetadata().getOwnerReferences().get(0), this));
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
