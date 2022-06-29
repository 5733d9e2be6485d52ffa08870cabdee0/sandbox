package com.redhat.service.smartevents.shard.operator.networking;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.redhat.service.smartevents.shard.operator.app.Platform;
import com.redhat.service.smartevents.shard.operator.app.PlatformConfigProvider;
import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;

import io.fabric8.openshift.client.OpenShiftClient;

@Singleton
public class NetworkingServiceProducer {

    @Inject
    OpenShiftClient client;

    @Inject
    PlatformConfigProvider platformConfigProvider;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Inject
    TemplateProvider templateProvider;

    @Produces
    public NetworkingService getService() {
        if (Platform.OPENSHIFT.equals(platformConfigProvider.getPlatform())) {
            return new OpenshiftNetworkingService(client, templateProvider, istioGatewayProvider);
        }
        return new KubernetesNetworkingService(client, templateProvider, istioGatewayProvider);
    }
}
