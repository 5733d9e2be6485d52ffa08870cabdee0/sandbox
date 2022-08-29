package com.redhat.service.smartevents.manager.dns.kubernetes;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.utils.Constants;
import com.redhat.service.smartevents.manager.TestConstants;

import static org.assertj.core.api.Assertions.assertThat;

public class DnsServiceKindImplTest {

    private static final String KIND_CONTROL_PLANE_ADDRESS = "kind-control-plane";

    @Test
    public void testKindDnsService() {
        DnsServiceKindImpl dnsServiceKind = new DnsServiceKindImpl(KIND_CONTROL_PLANE_ADDRESS);

        assertThat(dnsServiceKind.buildBridgeEndpoint(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .isEqualTo(Constants.HTTP_SCHEME + KIND_CONTROL_PLANE_ADDRESS + "/ob-" + TestConstants.DEFAULT_CUSTOMER_ID + "/ob-" + TestConstants.DEFAULT_BRIDGE_ID);
        assertThat(dnsServiceKind.createDnsRecord(TestConstants.DEFAULT_BRIDGE_ID)).isTrue();
        assertThat(dnsServiceKind.deleteDnsRecord(TestConstants.DEFAULT_BRIDGE_ID)).isTrue();
    }
}
