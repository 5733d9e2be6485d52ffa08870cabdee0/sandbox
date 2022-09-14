package com.redhat.service.smartevents.manager.dns.openshift;

public class DnsConfigOpenshiftProviderStub extends DnsConfigOpenshiftProviderImpl {

    public DnsConfigOpenshiftProviderStub(String subdomain, String hostedZoneName, String hostedZoneId, String awsAccessKeyId, String awsSecretAccessKey) {
        super(subdomain, hostedZoneName, awsAccessKeyId, awsSecretAccessKey);
        setHostedZoneId(hostedZoneId);
    }

    @Override
    protected String retrieveHostedZoneId(String hostedZoneName) {
        return null;
    }
}
