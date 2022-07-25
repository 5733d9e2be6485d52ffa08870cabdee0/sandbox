package com.redhat.service.smartevents.manager.dns.kubernetes;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.TestConstants;

import static org.assertj.core.api.Assertions.assertThat;

public class DnsServiceMinikubeImplTest {

    private static final String MINIKUBE_IP = "192.168.2.1";

    @Test
    public void test() {
        DnsServiceMinikubeImpl dnsServiceMinikube = new DnsServiceMinikubeImpl();
        dnsServiceMinikube.minikubeIp = MINIKUBE_IP;

        assertThat(dnsServiceMinikube.buildBridgeHost(TestConstants.DEFAULT_BRIDGE_ID)).isEqualTo(MINIKUBE_IP);
        assertThat(dnsServiceMinikube.createDnsRecord(TestConstants.DEFAULT_BRIDGE_ID)).isTrue();
        assertThat(dnsServiceMinikube.deleteDnsRecord(TestConstants.DEFAULT_BRIDGE_ID)).isTrue();
    }
}
