package com.redhat.service.bridge.shard.operator.watchers;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;

public class SecretEvent extends DefaultEvent {

    private final Watcher.Action action;
    private final Secret secret;

    public SecretEvent(Watcher.Action action, Secret resource, SecretEventSource secretEventSource) {
        // TODO: this mapping is really critical and should be made more explicit by the java operator sdk
        super(resource.getMetadata().getOwnerReferences().get(0).getUid(), secretEventSource);
        this.action = action;
        this.secret = resource;
    }

    public Watcher.Action getAction() {
        return action;
    }

    public String resourceUid() {
        return getSecret().getMetadata().getUid();
    }

    @Override
    public String toString() {
        return "SecretEvent{"
                + "action="
                + action
                + ", resource=[ name="
                + getSecret().getMetadata().getName()
                + ", kind="
                + getSecret().getKind()
                + ", apiVersion="
                + getSecret().getApiVersion()
                + " ,resourceVersion="
                + getSecret().getMetadata().getResourceVersion()
                + ", markedForDeletion: "
                + (getSecret().getMetadata().getDeletionTimestamp() != null
                        && !getSecret().getMetadata().getDeletionTimestamp().isEmpty())
                + " ]"
                + '}';
    }

    public Secret getSecret() {
        return secret;
    }
}
