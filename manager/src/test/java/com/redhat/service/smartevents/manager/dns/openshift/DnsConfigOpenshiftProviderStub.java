package com.redhat.service.smartevents.manager.dns.openshift;

public class DnsConfigOpenshiftProviderStub extends DnsConfigOpenshiftProviderImpl {

    private final String hostedZoneId;

    public DnsConfigOpenshiftProviderStub(String subdomain, String hostedZoneName, String hostedZoneId, String awsAccessKeyId, String awsSecretAccessKey) {
        super(subdomain, hostedZoneName, awsAccessKeyId, awsSecretAccessKey);
        this.hostedZoneId = hostedZoneId;
    }

    @Override
    protected String retrieveHostedZoneId(String hostedZoneName) {
        return this.hostedZoneId;
    }
}
