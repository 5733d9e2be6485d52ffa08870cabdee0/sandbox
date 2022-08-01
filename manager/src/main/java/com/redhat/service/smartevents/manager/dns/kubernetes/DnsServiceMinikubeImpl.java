package com.redhat.service.smartevents.manager.dns.kubernetes;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.manager.dns.DnsService;

import io.quarkus.arc.properties.IfBuildProperty;

@ApplicationScoped
@IfBuildProperty(name = "event-bridge.k8s.orchestrator", stringValue = "minikube")
public class DnsServiceMinikubeImpl implements DnsService {

    @ConfigProperty(name = "minikubeip")
    String minikubeIp;

    @Override
    public String buildBridgeEndpoint(String bridgeId, String customerId) {
        return "http://" + minikubeIp + "/ob-" + customerId + "/ob-" + bridgeId;
    }

    @Override
    public Boolean createDnsRecord(String bridgeId) {
        // Do nothing
        return true;
    }

    @Override
    public Boolean deleteDnsRecord(String bridgeId) {
        // Do nothing
        return true;
    }
}
