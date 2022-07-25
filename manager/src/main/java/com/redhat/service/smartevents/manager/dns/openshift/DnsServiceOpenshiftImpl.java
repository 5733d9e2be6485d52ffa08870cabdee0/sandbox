package com.redhat.service.smartevents.manager.dns.openshift;

import java.time.Duration;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.redhat.service.smartevents.manager.ShardService;
import com.redhat.service.smartevents.manager.dns.DnsService;

import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@IfBuildProperty(name = "event-bridge.k8s.orchestrator", stringValue = "openshift", enableIfMissing = true)
public class DnsServiceOpenshiftImpl implements DnsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsServiceOpenshiftImpl.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    DnsConfigOpenshiftProvider dnsConfigOpenshiftProvider;

    ShardService shardService;

    @Inject
    public DnsServiceOpenshiftImpl(ShardService shardService, DnsConfigOpenshiftProvider dnsConfigOpenshiftProvider) {
        this.shardService = shardService;
        this.dnsConfigOpenshiftProvider = dnsConfigOpenshiftProvider;
    }

    @Override
    public String buildBridgeHost(String bridgeId) {
        return bridgeId + dnsConfigOpenshiftProvider.getSubdomain();
    }

    /**
     * Creates a new DNS record in the hosted zone with the "simple" router policy. For complex DNS resolution we have to change this accordingly.
     *
     * @param bridgeId
     * @return
     */
    @Override
    public Boolean createDnsRecord(String bridgeId) {
        LOGGER.info(String.format("creating DNS zone for bridge '%s'", bridgeId));
        ResourceRecordSet resourceRecordSet = buildResourceRecordSet(buildBridgeHost(bridgeId), bridgeId);
        Change addStateChange = new Change(ChangeAction.CREATE, resourceRecordSet);
        ChangeBatch changeBatch = new ChangeBatch(List.of(addStateChange));
        return Uni.createFrom().future(
                dnsConfigOpenshiftProvider.getAmazonRouteClient()
                        .changeResourceRecordSetsAsync(new ChangeResourceRecordSetsRequest().withChangeBatch(changeBatch).withHostedZoneId(dnsConfigOpenshiftProvider.getHostedZoneId())))
                .onItem()
                .transformToUni(x -> Uni.createFrom().item(true))
                .await().atMost(DEFAULT_TIMEOUT);
    }

    @Override
    public Boolean deleteDnsRecord(String bridgeId) {
        ResourceRecordSet resourceRecordSet = buildResourceRecordSet(buildBridgeHost(bridgeId), bridgeId);
        Change addStateChange = new Change(ChangeAction.DELETE, resourceRecordSet);
        ChangeBatch changeBatch = new ChangeBatch(List.of(addStateChange));
        return Uni.createFrom().future(
                dnsConfigOpenshiftProvider.getAmazonRouteClient().changeResourceRecordSetsAsync(
                        new ChangeResourceRecordSetsRequest()
                                .withChangeBatch(changeBatch)
                                .withHostedZoneId(
                                        dnsConfigOpenshiftProvider.getHostedZoneId())))
                .onItem()
                .transformToUni(x -> Uni.createFrom().item(true))
                .await().atMost(DEFAULT_TIMEOUT);
    }

    private ResourceRecordSet buildResourceRecordSet(String recordName, String bridgeId) {
        ResourceRecordSet resourceRecordSet = new ResourceRecordSet()
                .withName(recordName)
                .withTTL(60L);
        resourceRecordSet.setType(RRType.CNAME);

        ResourceRecord resourceRecord = new ResourceRecord(shardService.getAssignedShard(bridgeId).getRouterCanonicalHostname());
        resourceRecordSet.setResourceRecords(List.of(resourceRecord));
        return resourceRecordSet;
    }
}
