package com.redhat.service.smartevents.manager.dns.openshift;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DnsConfigOpenshiftProviderImplTest {

    private static final String SUBDOMAIN = ".smartevents.bf2.dev";
    private static final String HOSTED_ZONE_ID = "ABCD";
    private static final String AWS_ACCESS_KEY_ID = "key_id";
    private static final String AWS_SECRET_ACCESS_KEY = "secret_key";

    @Test
    public void testConfigProvider() {
        DnsConfigOpenshiftProviderImpl dnsConfigProvider = new DnsConfigOpenshiftProviderImpl();
        dnsConfigProvider.subdomain = SUBDOMAIN;
        dnsConfigProvider.hostedZoneId = HOSTED_ZONE_ID;
        dnsConfigProvider.awsAccessKeyId = AWS_ACCESS_KEY_ID;
        dnsConfigProvider.awsSecretAccessKey = AWS_SECRET_ACCESS_KEY;

        dnsConfigProvider.init();

        assertThat(dnsConfigProvider.getSubdomain()).isEqualTo(SUBDOMAIN);
        assertThat(dnsConfigProvider.getHostedZoneId()).isEqualTo(HOSTED_ZONE_ID);
        assertThat(dnsConfigProvider.getAmazonRouteClient()).isNotNull();
    }
}
