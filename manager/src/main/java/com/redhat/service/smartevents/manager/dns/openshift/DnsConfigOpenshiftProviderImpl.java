package com.redhat.service.smartevents.manager.dns.openshift;

import java.time.Duration;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.route53.AmazonRoute53Async;
import com.amazonaws.services.route53.AmazonRoute53AsyncClientBuilder;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;

import io.quarkus.runtime.Quarkus;
import io.smallrye.mutiny.Uni;

public class DnsConfigOpenshiftProviderImpl implements DnsConfigOpenshiftProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsConfigOpenshiftProviderImpl.class);

    private final AmazonRoute53Async client;

    protected String subdomain;

    protected String hostedZoneId;

    protected String awsAccessKeyId;

    protected String awsSecretAccessKey;

    public DnsConfigOpenshiftProviderImpl() {
        this(ConfigProvider.getConfig().getValue("event-bridge.dns.subdomain", String.class),
                ConfigProvider.getConfig().getValue("event-bridge.dns.hosted-zone-name", String.class),
                ConfigProvider.getConfig().getValue("event-bridge.dns.aws.route53.access-key-id", String.class),
                ConfigProvider.getConfig().getValue("event-bridge.dns.aws.route53.secret-access-key", String.class));
    }

    public DnsConfigOpenshiftProviderImpl(String subdomain, String hostedZoneName, String awsAccessKeyId, String awsSecretAccessKey) {
        this.subdomain = subdomain;
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;

        BasicAWSCredentials awsCred = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);

        this.client = AmazonRoute53AsyncClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCred))
                .withRegion(Regions.DEFAULT_REGION)
                .build();
        this.hostedZoneId = retrieveHostedZoneId(hostedZoneName);
    }

    protected String retrieveHostedZoneId(String hostedZoneName) {
        ListHostedZonesResult listHostedZonesResult;
        try {
            listHostedZonesResult = Uni
                    .createFrom()
                    .future(this.client.listHostedZonesAsync())
                    .await()
                    .atMost(Duration.ofSeconds(20));
        } catch (RuntimeException e) {
            LOGGER.error("Failed to interact with AWS to retrieve the hosted zone id for the hosted zone with name '{}'. The application is going to stop.", hostedZoneName, e);
            Quarkus.asyncExit(1);
            return null;
        }

        Optional<HostedZone> hostedZoneOpt = listHostedZonesResult
                .getHostedZones()
                .stream()
                .filter(x -> hostedZoneName.equals(x.getName()))
                .findFirst();

        if (hostedZoneOpt.isEmpty()) {
            LOGGER.error("Hosted zone with name '{}' not found. The application is going to stop.", hostedZoneName);
            Quarkus.asyncExit(1);
            return null;
        }

        LOGGER.info("Hosted zone id for the hosted zone '{}' is '{}'", hostedZoneName, hostedZoneOpt.get().getId());
        return hostedZoneOpt.get().getId();
    }

    protected void setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
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
