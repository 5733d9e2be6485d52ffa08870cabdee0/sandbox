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
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;

@ApplicationScoped
public class BridgeWorker extends AbstractWorker<Bridge> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeWorker.class);

    private static final String BRIDGE_WORKER_CLASSNAME = Bridge.class.getName();

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    RhoasService rhoasService;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

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

        Callable<Topic> createTopicCallable = () -> rhoasService.createTopicAndGrantAccessFor(resourceNamesProvider.getBridgeTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        execute(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME,
                bridge,
                createTopicCallable,
                defaultOnResult(),
                defaultOnException());

        Callable<Boolean> createDNSEntryCallable = () -> dnsService.createDnsRecord(bridge.getId());
        execute(DefaultConditions.CP_DNS_RECORD_READY_NAME,
                bridge,
                createDNSEntryCallable,
                defaultOnResult(),
                defaultOnException());

        return persist(bridge);
    }

    @Override
    public Bridge deleteDependencies(Work work, Bridge bridge) {
        LOGGER.info("Destroying dependencies for '{}' [{}]",
                bridge.getName(),
                bridge.getId());

        LOGGER.info("Deleting topics for bridge '{}' [{}]...", bridge.getName(), bridge.getId());

        Callable<Void> deleteTopicCallable = () -> rhoasService.deleteTopicAndRevokeAccessFor(resourceNamesProvider.getBridgeTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        execute(DefaultConditions.CP_KAFKA_TOPIC_DELETED_NAME,
                bridge,
                deleteTopicCallable,
                defaultOnResult(),
                defaultOnException());

        Callable<Boolean> deleteDNSEntryCallable = () -> dnsService.deleteDnsRecord(bridge.getId());
        execute(DefaultConditions.CP_DNS_RECORD_DELETED_NAME,
                bridge,
                deleteDNSEntryCallable,
                defaultOnResult(),
                defaultOnException());

        return persist(bridge);
    }

    @Override
    public boolean accept(Work work) {
        return BRIDGE_WORKER_CLASSNAME.equals(work.getType());
    }
}
