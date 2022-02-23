package com.redhat.service.bridge.manager;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.ManagedEntityStatus;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.dao.PreparingWorkerDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.BridgePreparingSubStatus;
import com.redhat.service.bridge.manager.models.PreparingWorker;
import com.redhat.service.bridge.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.bridge.manager.providers.WorkerIdProvider;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 * Accepted -> Preparing -> topic requested -> topic created -> provisioning
 */

@ApplicationScoped
public class BridgePreparingWorker implements AbstractPreparingWorker<Bridge> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgePreparingWorker.class);

    private static final String CREATE_KAFKA_TOPIC = "bridge-accepted";

    @Inject
    EventBus eventBus;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    PreparingWorkerDAO preparingWorkerDAO;

    @Inject
    RhoasService rhoasService;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Inject
    WorkerIdProvider workerIdProvider;

    @Override
    public void accept(Bridge b) {
        LOGGER.info("Accept entity " + b.getId());
        if (b.getStatus() != null) {
            LOGGER.warn("The status of the entity is not null. It will be overwritten.");
        }
        b.setStatus(ManagedEntityStatus.ACCEPTED);
        b.setDesiredStatus(ManagedEntityStatus.PREPARING);

        // Persist and fire
        transact(b);

        PreparingWorker preparingWorker = new PreparingWorker();
        preparingWorker.setEntityId(b.getId());
        preparingWorker.setStatus(null);
        preparingWorker.setDesiredStatus(BridgePreparingSubStatus.TOPIC_CREATED.toString()); // where you want to go. i.e. step 1
        preparingWorker.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        preparingWorker.setModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
        transact(preparingWorker);

        eventBus.requestAndForget(CREATE_KAFKA_TOPIC, b);
    }

    @Transactional
    @ConsumeEvent(value = CREATE_KAFKA_TOPIC, blocking = true)
    public void step1_createKafkaTopic(Bridge bridge) {
        try {
            rhoasService.createTopicAndGrantAccessFor(internalKafkaConfigurationProvider.getBridgeTopicName(bridge), RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to create the topic. The retry will be performed by the reschedule loop.", e);
            PreparingWorker preparingWorker = preparingWorkerDAO.findByEntityId(bridge.getId()).get(0);
            preparingWorker.setModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            preparingWorker.setStatus(BridgePreparingSubStatus.FAILURE.toString()); // where you are. The rescheduler will fire the event according to the desiredStatus
            transact(preparingWorker);
            return;
        }

        PreparingWorker preparingWorker = preparingWorkerDAO.findByEntityId(bridge.getId()).get(0);
        preparingWorkerDAO.deleteById(preparingWorker.getId());

        // The bridge is ready to be provisioned by the shard.
        bridge.setStatus(ManagedEntityStatus.PREPARING);
        bridge.setDesiredStatus(ManagedEntityStatus.PROVISIONING);
        transact(bridge);
    }

    @Override
    @Scheduled(every = "30s")
    public void reschedule() {
        LOGGER.info("resched");
        List<PreparingWorker> preparingWorkers = preparingWorkerDAO.findByWorkerIdAndStatusAndType(workerIdProvider.getWorkerId(), BridgePreparingSubStatus.FAILURE.toString(), "BRIDGE");
        for (PreparingWorker preparingWorker : preparingWorkers) {
            Bridge bridge = bridgeDAO.findById(preparingWorker.getEntityId());
            if (BridgePreparingSubStatus.TOPIC_REQUESTED.equals(BridgePreparingSubStatus.valueOf(preparingWorker.getDesiredStatus()))) {
                eventBus.requestAndForget(CREATE_KAFKA_TOPIC, bridge);
            } else {
                LOGGER.error("BridgePreparingWorker can't process failed worker with desired status " + preparingWorker.getDesiredStatus());
            }
        }
    }

    @Override
    @Scheduled(every = "5m")
    public void discoverOrphanWorkers() {
        LOGGER.info("discovering orphan");
        ZonedDateTime orphans = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(30); // entities older than 30 minutes will be processed with an override of the worker_id.
        List<PreparingWorker> preparingWorkers = preparingWorkerDAO.findByAgeAndStatusAndType(orphans, BridgePreparingSubStatus.FAILURE.toString(), "BRIDGE");
        for (PreparingWorker preparingWorker : preparingWorkers) {
            Bridge bridge = bridgeDAO.findById(preparingWorker.getEntityId());
            if (BridgePreparingSubStatus.TOPIC_REQUESTED.equals(BridgePreparingSubStatus.valueOf(preparingWorker.getDesiredStatus()))) {
                eventBus.requestAndForget(CREATE_KAFKA_TOPIC, bridge);
            } else {
                LOGGER.error("BridgePreparingWorker can't process failed worker with desired status " + preparingWorker.getDesiredStatus());
            }
        }
    }

    @Transactional
    private void transact(Bridge b) {
        bridgeDAO.getEntityManager().merge(b);
    }

    @Transactional
    private void transact(PreparingWorker b) {
        preparingWorkerDAO.getEntityManager().merge(b);
    }

}
