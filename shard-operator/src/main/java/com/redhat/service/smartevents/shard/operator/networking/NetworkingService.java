package com.redhat.service.smartevents.shard.operator.networking;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;

import io.javaoperatorsdk.operator.processing.event.source.EventSource;

public interface NetworkingService {

    NetworkResource fetchOrCreateNetworkIngress(BridgeIngress bridgeIngress);

    EventSource buildInformerEventSource(String component);

    boolean delete(String name, String namespace);
}
