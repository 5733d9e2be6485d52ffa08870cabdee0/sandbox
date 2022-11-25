package com.redhat.service.smartevents.shard.operator.core.networking;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

public interface NetworkingService {

    // TODO: https://issues.redhat.com/browse/MGDOBR-995 refactor as we don't need anymore NetworkResource
    NetworkResource fetchOrCreateBrokerNetworkIngress(HasMetadata bridgeIngress, Secret secret, String path);

    EventSource buildInformerEventSource(String operatorName, String component);

    boolean delete(String name, String namespace);
}
