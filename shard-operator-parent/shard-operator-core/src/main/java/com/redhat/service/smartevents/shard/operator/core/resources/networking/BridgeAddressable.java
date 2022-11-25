package com.redhat.service.smartevents.shard.operator.core.resources.networking;

import io.fabric8.kubernetes.api.model.HasMetadata;

/*
    Marker interface that provides a common way of exposing the required DNS host entry
    from various different CRDS.
 */
public interface BridgeAddressable extends HasMetadata {
    String getIngressHost();
}
