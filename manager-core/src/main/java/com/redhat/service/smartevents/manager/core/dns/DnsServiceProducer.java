package com.redhat.service.smartevents.manager.core.dns;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.redhat.service.smartevents.infra.core.app.Orchestrator;
import com.redhat.service.smartevents.infra.core.app.OrchestratorConfigProvider;
import com.redhat.service.smartevents.manager.core.dns.kubernetes.DnsServiceKindImpl;
import com.redhat.service.smartevents.manager.core.dns.kubernetes.DnsServiceMinikubeImpl;
import com.redhat.service.smartevents.manager.core.dns.openshift.DnsConfigOpenshiftProvider;
import com.redhat.service.smartevents.manager.core.dns.openshift.DnsConfigOpenshiftProviderImpl;
import com.redhat.service.smartevents.manager.core.dns.openshift.DnsServiceOpenshiftImpl;
import com.redhat.service.smartevents.manager.core.services.ShardService;

@Singleton
public class DnsServiceProducer {

    @Inject
    OrchestratorConfigProvider orchestratorConfigProvider;

    @Inject
    ShardService shardService;

    @Produces
    @ApplicationScoped
    public DnsService init() {
        if (Orchestrator.OPENSHIFT.equals(orchestratorConfigProvider.getOrchestrator())) {
            DnsConfigOpenshiftProvider dnsConfigOpenshiftProvider = new DnsConfigOpenshiftProviderImpl();
            return new DnsServiceOpenshiftImpl(shardService, dnsConfigOpenshiftProvider);
        }
        if (Orchestrator.MINIKUBE.equals(orchestratorConfigProvider.getOrchestrator())) {
            return new DnsServiceMinikubeImpl();
        }
        if (Orchestrator.KIND.equals(orchestratorConfigProvider.getOrchestrator())) {
            return new DnsServiceKindImpl();
        }
        throw new IllegalArgumentException(String.format("Invalid orchestrator configuration '%s'.", orchestratorConfigProvider.getOrchestrator()));
    }
}
