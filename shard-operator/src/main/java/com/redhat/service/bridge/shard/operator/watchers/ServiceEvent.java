package com.redhat.service.bridge.shard.operator.watchers;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;

public class ServiceEvent extends DefaultEvent {

    private final Watcher.Action action;
    private final Service service;

    public ServiceEvent(Watcher.Action action, Service service, ServiceEventSource serviceEventSource) {
        // TODO: this mapping is really critical and should be made more explicit by the java operator sdk
        super(service.getMetadata().getOwnerReferences().get(0).getUid(), serviceEventSource);
        this.action = action;
        this.service = service;
    }

    public Watcher.Action getAction() {
        return action;
    }

    public String resourceUid() {
        return getService().getMetadata().getUid();
    }

    @Override
    public String toString() {
        return "ServiceEvent{"
                + "action="
                + action
                + ", resource=[ name="
                + getService().getMetadata().getName()
                + ", kind="
                + getService().getKind()
                + ", apiVersion="
                + getService().getApiVersion()
                + " ,resourceVersion="
                + getService().getMetadata().getResourceVersion()
                + ", markedForDeletion: "
                + (getService().getMetadata().getDeletionTimestamp() != null
                        && !getService().getMetadata().getDeletionTimestamp().isEmpty())
                + " ]"
                + '}';
    }

    public Service getService() {
        return service;
    }
}
