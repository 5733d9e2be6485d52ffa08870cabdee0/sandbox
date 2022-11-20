package com.redhat.service.smartevents.shard.operator.networking;

import com.redhat.service.smartevents.infra.app.Orchestrator;
import com.redhat.service.smartevents.infra.app.OrchestratorConfigProvider;
import io.fabric8.openshift.client.OpenShiftClient;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NetworkingServiceProducer {

    @Inject
    OpenShiftClient client;

    @Inject
    OrchestratorConfigProvider orchestratorConfigProvider;

    @Produces
    public NetworkingService getService() {
        if (Orchestrator.OPENSHIFT.equals(orchestratorConfigProvider.getOrchestrator())) {
            return new OpenshiftNetworkingService(client);
        }
        return new KubernetesNetworkingService(client);
    }
}
