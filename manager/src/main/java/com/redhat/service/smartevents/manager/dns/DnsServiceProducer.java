package com.redhat.service.smartevents.manager.dns;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.manager.ShardService;
import com.redhat.service.smartevents.manager.dns.kubernetes.DnsServiceKindImpl;
import com.redhat.service.smartevents.manager.dns.kubernetes.DnsServiceMinikubeImpl;
import com.redhat.service.smartevents.manager.dns.openshift.DnsConfigOpenshiftProvider;
import com.redhat.service.smartevents.manager.dns.openshift.DnsConfigOpenshiftProviderImpl;
import com.redhat.service.smartevents.manager.dns.openshift.DnsServiceOpenshiftImpl;

@Dependent
public class DnsServiceProducer {

    @ConfigProperty(name = "event-bridge.k8s.orchestrator")
    String orchestrator;

    @Inject
    ShardService shardService;

    @Produces
    public DnsService init() {
        if (orchestrator.equals("openshift")) {
            DnsConfigOpenshiftProvider dnsConfigOpenshiftProvider = new DnsConfigOpenshiftProviderImpl();
            return new DnsServiceOpenshiftImpl(shardService, dnsConfigOpenshiftProvider);
        }
        if (orchestrator.equals("minikube")) {
            return new DnsServiceMinikubeImpl();
        }
        if (orchestrator.equals("kind")) {
            return new DnsServiceKindImpl();
        }
        throw new IllegalArgumentException("Invalid 'event-bridge.k8s.orchestrator' property value.");
    }
}
