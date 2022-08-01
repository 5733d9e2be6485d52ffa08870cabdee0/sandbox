package com.redhat.service.smartevents.manager.dns.openshift;

import org.eclipse.microprofile.config.ConfigProvider;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.route53.AmazonRoute53Async;
import com.amazonaws.services.route53.AmazonRoute53AsyncClientBuilder;

public class DnsConfigOpenshiftProviderImpl implements DnsConfigOpenshiftProvider {

    private final AmazonRoute53Async client;

    protected String subdomain;

    protected String hostedZoneId;

    protected String awsAccessKeyId;

    protected String awsSecretAccessKey;

    public DnsConfigOpenshiftProviderImpl() {
        this(ConfigProvider.getConfig().getValue("event-bridge.dns.subdomain", String.class),
                ConfigProvider.getConfig().getValue("event-bridge.dns.hosted-zone-id", String.class),
                ConfigProvider.getConfig().getValue("event-bridge.dns.aws.route53.access-key-id", String.class),
                ConfigProvider.getConfig().getValue("event-bridge.dns.aws.route53.secret-access-key", String.class));
    }

    public DnsConfigOpenshiftProviderImpl(String subdomain, String hostedZoneId, String awsAccessKeyId, String awsSecretAccessKey) {
        this.subdomain = subdomain;
        this.hostedZoneId = hostedZoneId;
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;

        BasicAWSCredentials awsCred = new BasicAWSCredentials(this.awsAccessKeyId, this.awsSecretAccessKey);

        this.client = AmazonRoute53AsyncClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCred))
                .withRegion(Regions.DEFAULT_REGION)
                .build();
    }

    @Override
    public AmazonRoute53Async getAmazonRouteClient() {
        return this.client;
    }

    @Override
    public String getSubdomain() {
        return this.subdomain;
    }

    @Override
    public String getHostedZoneId() {
        return hostedZoneId;
    }
}
