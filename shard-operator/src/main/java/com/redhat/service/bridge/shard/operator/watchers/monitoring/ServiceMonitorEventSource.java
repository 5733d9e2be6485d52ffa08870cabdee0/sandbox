package com.redhat.service.bridge.shard.operator.watchers.monitoring;

import java.util.Optional;

import com.redhat.service.bridge.shard.operator.monitoring.ServiceMonitorClient;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;
import com.redhat.service.bridge.shard.operator.watchers.BaseEventSource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;

public class ServiceMonitorEventSource extends BaseEventSource<ServiceMonitor, ServiceMonitorEvent> {

    private final KubernetesClient client;

    public static Optional<ServiceMonitorEventSource> createAndRegisterWatch(KubernetesClient client, String component) {
        if (ServiceMonitorClient.isServiceMonitorAvailable(client)) {
            final ServiceMonitorEventSource serviceMonitorEventSource = new ServiceMonitorEventSource(client, component);
            serviceMonitorEventSource.registerWatch(component);
            return Optional.of(serviceMonitorEventSource);
        }
        return Optional.empty();
    }

    private ServiceMonitorEventSource(KubernetesClient client, String component) {
        super(component);
        this.client = client;
    }

    @Override
    protected ServiceMonitorEvent newEvent(Action action, ServiceMonitor resource) {
        return new ServiceMonitorEvent(action, resource, this);
    }

    @Override
    protected void registerWatch(String component) {
        ServiceMonitorClient.get(client)
                .inAnyNamespace()
                .withLabels(new LabelsBuilder()
                        .withManagedByOperator()
                        .withComponent(component)
                        .build())
                .watch(this);
    }
}
