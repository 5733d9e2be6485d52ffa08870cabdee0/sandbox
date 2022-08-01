package com.redhat.service.smartevents.shard.operator.networking;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

public interface NetworkingService {

    // TODO: refactor as we don't need anymore NetworkResource
    NetworkResource fetchOrCreateBrokerNetworkIngress(BridgeIngress bridgeIngress, Secret secret, String path);

    EventSource buildInformerEventSource(String component);

    boolean delete(String name, String namespace);
}
