package com.redhat.service.bridge.manager.workers.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.bridge.manager.workers.Work;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.vertx.ConsumeEvent;

/*
    Deploys or deletes dependencies for a Bridge i.e. it creates/deletes a Topic and manages ACL for the topic
 */
@ApplicationScoped
public class BridgeWorker extends AbstractWorker<Bridge> {

    @Inject
    private RhoasService rhoasService;

    @Inject
    private BridgeDAO bridgeDAO;

    @Inject
    private InternalKafkaConfigurationProvider kafkaConfigurationProvider;

    @Override
    PanacheRepositoryBase<Bridge, String> getDao() {
        return bridgeDAO;
    }

    @Override
    void runCreateOfDependencies(Bridge managedResource) {
        String topicName = kafkaConfigurationProvider.getTopicName(managedResource);
        // Lets assume this is idempotent
        rhoasService.createTopicAndGrantAccessFor(topicName, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        managedResource.getDependencyStatus().setReady(true);
        // Hand over to the Shard
        managedResource.setStatus(BridgeStatus.PROVISIONING);
    }

    @Override
    void runDeleteOfDependencies(Bridge managedResource) {
        String topicName = kafkaConfigurationProvider.getTopicName(managedResource);
        // Lets assume this is idempotent
        rhoasService.deleteTopicAndRevokeAccessFor(topicName, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        managedResource.getDependencyStatus().setDeleted(true);
    }

    @ConsumeEvent(value = "Bridge", blocking = true)
    public void handleWork(Work work) {
        super.handleWork(work);
    }
}
