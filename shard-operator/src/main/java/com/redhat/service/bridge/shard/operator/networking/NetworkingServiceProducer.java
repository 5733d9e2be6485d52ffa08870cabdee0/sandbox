package com.redhat.service.bridge.shard.operator.networking;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.redhat.service.bridge.shard.operator.app.Platform;
import com.redhat.service.bridge.shard.operator.app.PlatformConfigProvider;

import io.fabric8.openshift.client.OpenShiftClient;

@Singleton
public class NetworkingServiceProducer {

    @Inject
    OpenShiftClient client;

    @Inject
    PlatformConfigProvider platformConfigProvider;

    @Produces
    public NetworkingService getService() {
        if (Platform.OPENSHIFT.equals(platformConfigProvider.getPlatform())) {
            return new OpenshiftNetworkingService(client);
        }
        return new KubernetesNetworkingService(client);
    }
}
