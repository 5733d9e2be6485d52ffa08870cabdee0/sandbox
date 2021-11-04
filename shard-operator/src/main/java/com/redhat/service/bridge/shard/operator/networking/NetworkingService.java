package com.redhat.service.bridge.shard.operator.networking;

import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;

public interface NetworkingService {

    NetworkResource fetchOrCreateNetworkIngress(Service service);

    AbstractEventSource createAndRegisterWatchNetworkResource(String applicationType);

    boolean delete(String name, String namespace);
}
