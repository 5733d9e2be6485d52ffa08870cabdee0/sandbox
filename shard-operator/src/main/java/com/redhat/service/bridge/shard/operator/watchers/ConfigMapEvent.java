package com.redhat.service.bridge.shard.operator.watchers;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;

public class ConfigMapEvent extends DefaultEvent {

    private final Watcher.Action action;
    private final ConfigMap configMap;

    public ConfigMapEvent(Watcher.Action action, ConfigMap resource, ConfigMapEventSource configMapEventSource) {
        // TODO: this mapping is really critical and should be made more explicit by the java operator sdk
        super(resource.getMetadata().getOwnerReferences().get(0).getUid(), configMapEventSource);
        this.action = action;
        this.configMap = resource;
    }

    public Watcher.Action getAction() {
        return action;
    }

    public String resourceUid() {
        return getConfigMap().getMetadata().getUid();
    }

    @Override
    public String toString() {
        return "ConfigMapEvent{"
                + "action="
                + action
                + ", resource=[ name="
                + getConfigMap().getMetadata().getName()
                + ", kind="
                + getConfigMap().getKind()
                + ", apiVersion="
                + getConfigMap().getApiVersion()
                + " ,resourceVersion="
                + getConfigMap().getMetadata().getResourceVersion()
                + ", markedForDeletion: "
                + (getConfigMap().getMetadata().getDeletionTimestamp() != null
                        && !getConfigMap().getMetadata().getDeletionTimestamp().isEmpty())
                + " ]"
                + '}';
    }

    public ConfigMap getConfigMap() {
        return configMap;
    }
}