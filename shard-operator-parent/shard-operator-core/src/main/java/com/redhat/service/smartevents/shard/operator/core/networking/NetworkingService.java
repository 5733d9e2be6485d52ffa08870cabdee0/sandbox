package com.redhat.service.smartevents.shard.operator.core.networking;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

public interface NetworkingService {

    // TODO: https://issues.redhat.com/browse/MGDOBR-995 refactor as we don't need anymore NetworkResource
    NetworkResource fetchOrCreateBrokerNetworkIngress(HasMetadata resource, Secret secret, String host, String path);

    EventSource buildInformerEventSource(EventSourceContext<?> eventSourceContext, String operatorName, String component);

    boolean delete(String name, String namespace);
}
