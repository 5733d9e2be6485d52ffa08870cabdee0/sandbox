package com.redhat.service.bridge.shard.operator.app;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.redhat.service.bridge.shard.operator.utils.networking.KubernetesNetworkingTestUtils;
import com.redhat.service.bridge.shard.operator.utils.networking.NetworkingTestUtils;
import com.redhat.service.bridge.shard.operator.utils.networking.OpenshiftNetworkingTestUtils;

import io.fabric8.openshift.client.OpenShiftClient;

@Singleton
public class NetworkingTestUtilsProvider {

    @Inject
    OpenShiftClient openShiftClient;

    @Inject
    PlatformConfigProvider platformConfigProvider;

    @Produces
    public NetworkingTestUtils produceNetworkingTestUtilsBean() {
        if (Platform.OPENSHIFT.equals(platformConfigProvider.getPlatform())) {
            return new OpenshiftNetworkingTestUtils(openShiftClient);
        }
        return new KubernetesNetworkingTestUtils(openShiftClient);
    }
}
