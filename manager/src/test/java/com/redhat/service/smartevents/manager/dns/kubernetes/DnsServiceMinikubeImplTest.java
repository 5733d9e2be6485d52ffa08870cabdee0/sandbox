package com.redhat.service.smartevents.manager.dns.kubernetes;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.TestConstants;

import static org.assertj.core.api.Assertions.assertThat;

public class DnsServiceMinikubeImplTest {

    private static final String MINIKUBE_IP = "192.168.2.1";

    @Test
    public void testMinikubeDnsService() {
        DnsServiceMinikubeImpl dnsServiceMinikube = new DnsServiceMinikubeImpl();
        dnsServiceMinikube.minikubeIp = MINIKUBE_IP;

        assertThat(dnsServiceMinikube.buildBridgeEndpoint(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .isEqualTo("http://" + MINIKUBE_IP + "/ob-" + TestConstants.DEFAULT_CUSTOMER_ID + "/ob-" + TestConstants.DEFAULT_BRIDGE_ID);
        assertThat(dnsServiceMinikube.createDnsRecord(TestConstants.DEFAULT_BRIDGE_ID)).isTrue();
        assertThat(dnsServiceMinikube.deleteDnsRecord(TestConstants.DEFAULT_BRIDGE_ID)).isTrue();
    }
}
