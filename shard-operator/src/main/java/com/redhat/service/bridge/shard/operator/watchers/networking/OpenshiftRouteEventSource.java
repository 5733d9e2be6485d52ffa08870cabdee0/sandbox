package com.redhat.service.bridge.shard.operator.watchers.networking;

import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;
import com.redhat.service.bridge.shard.operator.watchers.BaseEventSource;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenshiftRouteEventSource extends BaseEventSource<Route, NetworkResourceEvent> {

    private final OpenShiftClient client;

    public static OpenshiftRouteEventSource createAndRegisterWatch(OpenShiftClient client, String component) {
        OpenshiftRouteEventSource eventSource = new OpenshiftRouteEventSource(client, component);
        eventSource.registerWatch(component);
        return eventSource;
    }

    private OpenshiftRouteEventSource(OpenShiftClient client, String component) {
        super(component);
        this.client = client;
    }

    @Override
    protected NetworkResourceEvent newEvent(Action action, Route resource) {
        return new NetworkResourceEvent(action, resource.getMetadata().getOwnerReferences().get(0), this);
    }

    protected void registerWatch(String component) {
        client.routes().inAnyNamespace()
                .withLabels(
                        new LabelsBuilder()
                                .withManagedByOperator()
                                .withComponent(component)
                                .build())
                .watch(this);
    }
}
