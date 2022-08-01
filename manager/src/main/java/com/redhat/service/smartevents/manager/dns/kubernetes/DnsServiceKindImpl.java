package com.redhat.service.smartevents.manager.dns.kubernetes;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.manager.dns.DnsService;

import io.quarkus.arc.properties.IfBuildProperty;

@ApplicationScoped
@IfBuildProperty(name = "event-bridge.k8s.orchestrator", stringValue = "kind")
public class DnsServiceKindImpl implements DnsService {

    @ConfigProperty(name = "event-bridge.dns.ingress.override.hostname")
    String overrideHostname;

    @Override
    public String buildBridgeEndpoint(String bridgeId, String customerId) {
        return "http://" + overrideHostname + "/ob-" + customerId + "/ob-" + bridgeId;
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
