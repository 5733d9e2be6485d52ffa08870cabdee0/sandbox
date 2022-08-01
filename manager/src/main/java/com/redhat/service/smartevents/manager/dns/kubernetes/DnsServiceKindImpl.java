package com.redhat.service.smartevents.manager.dns.kubernetes;

import org.eclipse.microprofile.config.ConfigProvider;

import com.redhat.service.smartevents.manager.dns.DnsService;

public class DnsServiceKindImpl implements DnsService {

    private final String overrideHostname;

    public DnsServiceKindImpl() {
        this(ConfigProvider.getConfig().getValue("event-bridge.dns.ingress.override.hostname", String.class));
    }

    public DnsServiceKindImpl(String overrideHostname) {
        this.overrideHostname = overrideHostname;
    }

    @Override
    public String buildBridgeHost(String bridgeId) {
        return overrideHostname;
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
