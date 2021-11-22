package com.redhat.service.bridge.shard.operator.watchers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.quarkus.runtime.Quarkus;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;

public class ConfigMapEventSource extends AbstractEventSource implements Watcher<ConfigMap> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigMapEventSource.class);

    private final KubernetesClient client;

    private final String component;

    public static ConfigMapEventSource createAndRegisterWatch(KubernetesClient client, String component) {
        ConfigMapEventSource configMapEventSource = new ConfigMapEventSource(client, component);
        configMapEventSource.registerWatch(component);
        return configMapEventSource;
    }

    private ConfigMapEventSource(KubernetesClient client, String component) {
        this.client = client;
        this.component = component;
    }

    private void registerWatch(String component) {
        client
                .configMaps()
                .inAnyNamespace()
                .withLabels(
                        new LabelsBuilder()
                                .withManagedByOperator()
                                .withComponent(component)
                                .build())
                .watch(this);
    }

    @Override
    public void eventReceived(Watcher.Action action, ConfigMap configMap) {
        LOGGER.info(
                "Event received for action: '{}', ConfigMap: '{}'",
                action.name(),
                configMap.getMetadata().getName());

        if (action == Watcher.Action.ERROR) {
            LOGGER.warn(
                    "Skipping '{}' event for custom resource uid: '{}', version: '{}'",
                    action,
                    getUID(configMap),
                    getVersion(configMap));
            return;
        }

        eventHandler.handleEvent(new ConfigMapEvent(action, configMap, this));
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
