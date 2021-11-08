package com.redhat.service.bridge.shard.operator.watchers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.quarkus.runtime.Quarkus;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;

public class ServiceEventSource extends AbstractEventSource implements Watcher<Service> {
    private static final Logger log = LoggerFactory.getLogger(ServiceEventSource.class);

    private final KubernetesClient client;

    private final String component;

    public static ServiceEventSource createAndRegisterWatch(KubernetesClient client, String component) {
        ServiceEventSource serviceEventSource = new ServiceEventSource(client, component);
        serviceEventSource.registerWatch(component);
        return serviceEventSource;
    }

    private ServiceEventSource(KubernetesClient client, String component) {
        this.client = client;
        this.component = component;
    }

    private void registerWatch(String component) {
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

    @Override
    public void eventReceived(Watcher.Action action, Service service) {
        log.info(
                "Event received for action: {}, Service: {}",
                action.name(),
                service.getMetadata().getName());

        if (action == Watcher.Action.ERROR) {
            log.warn(
                    "Skipping {} event for custom resource uid: {}, version: {}",
                    action,
                    getUID(service),
                    getVersion(service));
            return;
        }

        eventHandler.handleEvent(new ServiceEvent(action, service, this));
    }

    @Override
    public void onClose(WatcherException e) {
        if (e == null) {
            log.warn("Unknown error happened with watch.");
            return;
        }
        if (e.isHttpGone()) {
            log.warn("Received error for watch, will try to reconnect.", e);
            registerWatch(this.component);
        } else {
            // Note that this should not happen normally, since fabric8 client handles reconnect.
            // In case it tries to reconnect this method is not called.
            log.error("Unexpected error happened with watch. Will exit.", e);
            Quarkus.asyncExit(1);
        }
    }
}
