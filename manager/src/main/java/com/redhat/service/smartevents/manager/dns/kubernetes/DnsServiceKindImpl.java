package com.redhat.service.smartevents.manager.dns.kubernetes;

import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.manager.dns.DnsService;

public class DnsServiceKindImpl implements DnsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsServiceKindImpl.class);

    private final String overrideHostname;

    public DnsServiceKindImpl() {
        this(ConfigProvider.getConfig().getValue("event-bridge.dns.ingress.override.hostname", String.class));
    }

    public DnsServiceKindImpl(String overrideHostname) {
        LOGGER.info("Using Kind implementation for DNS - the BridgeIngress endpoint is the kind control plane address and there is no interaction with AWS Route53.");
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
