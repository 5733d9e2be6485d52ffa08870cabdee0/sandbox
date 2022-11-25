package com.redhat.service.smartevents.shard.operator.core.networking;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.redhat.service.smartevents.infra.core.app.Orchestrator;
import com.redhat.service.smartevents.infra.core.app.OrchestratorConfigProvider;
import com.redhat.service.smartevents.shard.operator.core.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProvider;
import io.fabric8.openshift.client.OpenShiftClient;

@Singleton
public class NetworkingServiceProducer {

    @Inject
    OpenShiftClient client;

    @Inject
    OrchestratorConfigProvider orchestratorConfigProvider;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Inject
    TemplateProvider templateProvider;

    @Produces
    public NetworkingService getService() {
        if (Orchestrator.OPENSHIFT.equals(orchestratorConfigProvider.getOrchestrator())) {
            return new OpenshiftNetworkingService(client, templateProvider, istioGatewayProvider);
        }
        return new KubernetesNetworkingService(client, templateProvider, istioGatewayProvider);
    }
}
