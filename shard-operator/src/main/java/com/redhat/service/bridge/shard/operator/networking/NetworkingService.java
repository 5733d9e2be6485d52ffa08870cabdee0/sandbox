package com.redhat.service.bridge.shard.operator.networking;

import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

public interface NetworkingService {

    NetworkResource fetchOrCreateNetworkIngress(BridgeIngress bridgeIngress, Service service);

    EventSource buildInformerEventSource(String component);

    boolean delete(String name, String namespace);
}
