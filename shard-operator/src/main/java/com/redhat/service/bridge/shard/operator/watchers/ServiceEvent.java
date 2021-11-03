package com.redhat.service.bridge.shard.operator.watchers;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;

public class ServiceEvent extends DefaultEvent {

    private final Watcher.Action action;
    private final Service deployment;

    public ServiceEvent(Watcher.Action action, Service resource, ServiceEventSource serviceEventSource) {
        // TODO: this mapping is really critical and should be made more explicit by the java operator sdk
        super(resource.getMetadata().getOwnerReferences().get(0).getUid(), serviceEventSource);
        this.action = action;
        this.deployment = resource;
    }

    public Watcher.Action getAction() {
        return action;
    }

    public String resourceUid() {
        return getDeployment().getMetadata().getUid();
    }

    @Override
    public String toString() {
        return "CustomResourceEvent{"
                + "action="
                + action
                + ", resource=[ name="
                + getDeployment().getMetadata().getName()
                + ", kind="
                + getDeployment().getKind()
                + ", apiVersion="
                + getDeployment().getApiVersion()
                + " ,resourceVersion="
                + getDeployment().getMetadata().getResourceVersion()
                + ", markedForDeletion: "
                + (getDeployment().getMetadata().getDeletionTimestamp() != null
                        && !getDeployment().getMetadata().getDeletionTimestamp().isEmpty())
                + " ]"
                + '}';
    }

    public Service getDeployment() {
        return deployment;
    }
}
