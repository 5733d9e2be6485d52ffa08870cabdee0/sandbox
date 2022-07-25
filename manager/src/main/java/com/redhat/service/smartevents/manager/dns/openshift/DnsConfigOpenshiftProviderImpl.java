package com.redhat.service.smartevents.manager.dns.openshift;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.route53.AmazonRoute53Async;
import com.amazonaws.services.route53.AmazonRoute53AsyncClientBuilder;

import io.quarkus.arc.properties.IfBuildProperty;

@ApplicationScoped
@IfBuildProperty(name = "event-bridge.k8s.orchestrator", stringValue = "openshift", enableIfMissing = true)
public class DnsConfigOpenshiftProviderImpl implements DnsConfigOpenshiftProvider {

    private AmazonRoute53Async client;

    @ConfigProperty(name = "event-bridge.dns.subdomain")
    protected String subdomain;

    @ConfigProperty(name = "event-bridge.dns.hosted-zone-id")
    protected String hostedZoneId;

    @ConfigProperty(name = "quarkus.secretsmanager.aws.credentials.static-provider.access-key-id")
    String awsAccessKeyId;

    @ConfigProperty(name = "quarkus.secretsmanager.aws.credentials.static-provider.secret-access-key")
    String awsSecretAccessKey;

    @PostConstruct
    void init() {
        BasicAWSCredentials awsCred = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);

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
