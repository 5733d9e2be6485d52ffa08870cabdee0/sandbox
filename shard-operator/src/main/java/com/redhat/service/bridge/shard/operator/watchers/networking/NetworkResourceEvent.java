package com.redhat.service.bridge.shard.operator.watchers.networking;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;

public class NetworkResourceEvent extends DefaultEvent {

    private final Watcher.Action action;
    private final OwnerReference ownerReference;

    public NetworkResourceEvent(Watcher.Action action, OwnerReference ownerReference, AbstractEventSource networkEventSource) {
        // TODO: this mapping is really critical and should be made more explicit by the java operator sdk
        super(ownerReference.getUid(), networkEventSource);
        this.action = action;
        this.ownerReference = ownerReference;
    }

    public Watcher.Action getAction() {
        return action;
    }

    public String resourceUid() {
        return ownerReference.getUid();
    }

    @Override
    public String toString() {
        return "NetworkResourceEvent{"
                + "action="
                + action
                + ", ownerUid="
                + ownerReference.getUid()
                + '}';
    }
}
