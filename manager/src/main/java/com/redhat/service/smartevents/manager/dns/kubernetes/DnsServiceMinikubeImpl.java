package com.redhat.service.smartevents.manager.dns.kubernetes;

import org.eclipse.microprofile.config.ConfigProvider;

import com.redhat.service.smartevents.manager.dns.DnsService;

public class DnsServiceMinikubeImpl implements DnsService {

    private final String minikubeIp;

    public DnsServiceMinikubeImpl() {
        this(ConfigProvider.getConfig().getValue("minikubeip", String.class));
    }

    public DnsServiceMinikubeImpl(String minikubeIp) {
        this.minikubeIp = minikubeIp;
    }

    @Override
    public String buildBridgeHost(String bridgeId) {
        return minikubeIp;
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
