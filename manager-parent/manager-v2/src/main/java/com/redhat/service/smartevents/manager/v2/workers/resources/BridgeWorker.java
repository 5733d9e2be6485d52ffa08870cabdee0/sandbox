package com.redhat.service.smartevents.manager.v2.workers.resources;

import java.util.concurrent.Callable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.manager.core.dns.DnsService;
import com.redhat.service.smartevents.manager.core.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.core.services.RhoasService;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ManagedResourceV2DAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.services.BridgeService;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;

@ApplicationScoped
public class BridgeWorker extends AbstractWorker<Bridge> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeWorker.class);

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    RhoasService rhoasService;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    BridgeService bridgeService;

    @Inject
    DnsService dnsService;

    @Override
    public ManagedResourceV2DAO<Bridge> getDao() {
        return bridgeDAO;
    }

    @Override
    public String getId(Work work) {
        // The ID of the ManagedResource to process is stored directly in the JobDetail.
        return work.getManagedResourceId();
    }

    @Override
    public Bridge createDependencies(Work work, Bridge bridge) {
        LOGGER.info("Creating dependencies for '{}' [{}]",
                bridge.getName(),
                bridge.getId());

        // If this call throws an exception the Bridge's dependencies will be left in PROVISIONING state...
        Callable<Topic> createTopicCallable = () -> rhoasService.createTopicAndGrantAccessFor(resourceNamesProvider.getBridgeTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        executeWithFailureRecording(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, bridge, createTopicCallable, defaultOnResult(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME),
                defaultOnException(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME));

        // Create DNS record
        Callable<Boolean> createDNSEntryCallable = () -> dnsService.createDnsRecord(bridge.getId());
        executeWithFailureRecording(DefaultConditions.CP_DNS_RECORD_READY_NAME, bridge, createDNSEntryCallable, defaultOnResult(DefaultConditions.CP_DNS_RECORD_READY_NAME),
                defaultOnException(DefaultConditions.CP_DNS_RECORD_READY_NAME));

        return persist(bridge);
    }

    @Override
    public Bridge deleteDependencies(Work work, Bridge bridge) {
        LOGGER.info("Destroying dependencies for '{}' [{}]",
                bridge.getName(),
                bridge.getId());

        LOGGER.info("Deleting topics for bridge '{}' [{}]...", bridge.getName(), bridge.getId());

        // If this call throws an exception the Bridge's dependencies will be left in DELETING state...
        Callable<Void> deleteTopicCallable = () -> rhoasService.deleteTopicAndRevokeAccessFor(resourceNamesProvider.getBridgeTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        executeWithFailureRecording(DefaultConditions.CP_KAFKA_TOPIC_DELETED_NAME, bridge, deleteTopicCallable, defaultOnResult(DefaultConditions.CP_KAFKA_TOPIC_DELETED_NAME),
                defaultOnException(DefaultConditions.CP_KAFKA_TOPIC_DELETED_NAME));

        // Delete DNS entry
        Callable<Boolean> deleteDNSEntryCallable = () -> dnsService.deleteDnsRecord(bridge.getId());
        executeWithFailureRecording(DefaultConditions.CP_DNS_RECORD_DELETED_NAME, bridge, deleteDNSEntryCallable, defaultOnResult(DefaultConditions.CP_DNS_RECORD_DELETED_NAME),
                defaultOnException(DefaultConditions.CP_DNS_RECORD_DELETED_NAME));

        return persist(bridge);
    }
}
