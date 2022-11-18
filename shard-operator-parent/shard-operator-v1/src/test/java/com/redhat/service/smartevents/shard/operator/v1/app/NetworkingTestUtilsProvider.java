package com.redhat.service.smartevents.shard.operator.v1.app;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.redhat.service.smartevents.infra.core.app.Orchestrator;
import com.redhat.service.smartevents.infra.core.app.OrchestratorConfigProvider;
import com.redhat.service.smartevents.shard.operator.v1.utils.networking.KubernetesNetworkingTestUtils;
import com.redhat.service.smartevents.shard.operator.v1.utils.networking.NetworkingTestUtils;
import com.redhat.service.smartevents.shard.operator.v1.utils.networking.OpenshiftNetworkingTestUtils;

import io.fabric8.openshift.client.OpenShiftClient;

@Singleton
public class NetworkingTestUtilsProvider {

    @Inject
    OpenShiftClient openShiftClient;

    @Inject
    OrchestratorConfigProvider orchestratorConfigProvider;

    @Produces
    public NetworkingTestUtils produceNetworkingTestUtilsBean() {
        if (Orchestrator.OPENSHIFT.equals(orchestratorConfigProvider.getOrchestrator())) {
            return new OpenshiftNetworkingTestUtils(openShiftClient);
        }
        return new KubernetesNetworkingTestUtils(openShiftClient);
    }
}
