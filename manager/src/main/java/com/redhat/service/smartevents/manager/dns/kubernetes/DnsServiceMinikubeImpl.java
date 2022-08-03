package com.redhat.service.smartevents.manager.dns.kubernetes;

import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.utils.Constants;
import com.redhat.service.smartevents.manager.dns.DnsService;
import com.redhat.service.smartevents.manager.dns.KnativeBrokerPathBuilder;

public class DnsServiceMinikubeImpl implements DnsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsServiceMinikubeImpl.class);

    private final String minikubeIp;

    public DnsServiceMinikubeImpl() {
        this(ConfigProvider.getConfig().getValue("minikubeip", String.class));
    }

    public DnsServiceMinikubeImpl(String minikubeIp) {
        LOGGER.info("Using Minikube implementation for DNS - the BridgeIngress endpoint is the minikube address and there is no interaction with AWS Route53.");
        this.minikubeIp = minikubeIp;
    }

    @Override
    public String buildBridgeEndpoint(String bridgeId, String customerId) {
        return Constants.HTTP_SCHEME + minikubeIp + KnativeBrokerPathBuilder.build(customerId, bridgeId);
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
