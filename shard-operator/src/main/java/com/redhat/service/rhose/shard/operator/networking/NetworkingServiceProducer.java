package com.redhat.service.rhose.shard.operator.networking;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.redhat.service.rhose.shard.operator.app.Platform;
import com.redhat.service.rhose.shard.operator.app.PlatformConfigProvider;
import com.redhat.service.rhose.shard.operator.providers.TemplateProvider;

import io.fabric8.openshift.client.OpenShiftClient;

@Singleton
public class NetworkingServiceProducer {

    @Inject
    OpenShiftClient client;

    @Inject
    PlatformConfigProvider platformConfigProvider;

    @Inject
    TemplateProvider templateProvider;

    @Produces
    public NetworkingService getService() {
        if (Platform.OPENSHIFT.equals(platformConfigProvider.getPlatform())) {
            return new OpenshiftNetworkingService(client, templateProvider);
        }
        return new KubernetesNetworkingService(client, templateProvider);
    }
}
