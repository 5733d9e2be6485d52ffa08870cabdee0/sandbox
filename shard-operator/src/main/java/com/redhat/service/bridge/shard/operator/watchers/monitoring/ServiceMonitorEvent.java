package com.redhat.service.bridge.shard.operator.watchers.monitoring;

import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;

public class ServiceMonitorEvent extends DefaultEvent {

    private final Watcher.Action action;
    private final ServiceMonitor serviceMonitor;

    public ServiceMonitorEvent(Watcher.Action action, ServiceMonitor resource, ServiceMonitorEventSource serviceMonitorEventSource) {
        super(resource.getMetadata().getOwnerReferences().get(0).getUid(), serviceMonitorEventSource);
        this.action = action;
        this.serviceMonitor = resource;
    }

    public Watcher.Action getAction() {
        return action;
    }

    public String resourceUid() {
        return serviceMonitor.getMetadata().getUid();
    }

    @Override
    public String toString() {
        return "ServiceMonitorEvent{"
                + "action="
                + action
                + ", resource=[ name="
                + getServiceMonitor().getMetadata().getName()
                + ", kind="
                + getServiceMonitor().getKind()
                + ", apiVersion="
                + getServiceMonitor().getApiVersion()
                + " ,resourceVersion="
                + getServiceMonitor().getMetadata().getResourceVersion()
                + ", markedForDeletion: "
                + (getServiceMonitor().getMetadata().getDeletionTimestamp() != null
                        && !getServiceMonitor().getMetadata().getDeletionTimestamp().isEmpty())
                + " ]"
                + '}';
    }

    public ServiceMonitor getServiceMonitor() {
        return serviceMonitor;
    }
}
