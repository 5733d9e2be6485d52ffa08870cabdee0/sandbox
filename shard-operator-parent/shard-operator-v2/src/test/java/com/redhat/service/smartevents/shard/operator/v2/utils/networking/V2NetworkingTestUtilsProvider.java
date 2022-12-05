package com.redhat.service.smartevents.shard.operator.v2.utils.networking;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.redhat.service.smartevents.infra.core.app.Orchestrator;
import com.redhat.service.smartevents.infra.core.app.OrchestratorConfigProvider;

import io.fabric8.openshift.client.OpenShiftClient;

@Singleton
public class V2NetworkingTestUtilsProvider {

    @Inject
    OpenShiftClient openShiftClient;

    @Inject
    OrchestratorConfigProvider orchestratorConfigProvider;

    @Produces
    public V2NetworkingTestUtils produceNetworkingTestUtilsBean() {
        if (Orchestrator.OPENSHIFT.equals(orchestratorConfigProvider.getOrchestrator())) {
            return new V2OpenshiftNetworkingTestUtils(openShiftClient);
        }
        return new V2KubernetesNetworkingTestUtils(openShiftClient);
    }
}
