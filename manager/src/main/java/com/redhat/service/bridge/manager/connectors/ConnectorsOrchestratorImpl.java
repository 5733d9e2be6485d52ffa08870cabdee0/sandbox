package com.redhat.service.bridge.manager.connectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;

import static com.redhat.service.bridge.manager.connectors.Events.CONNECTOR_ACCEPTED_EVENT;
import static com.redhat.service.bridge.manager.connectors.Events.CONNECTOR_DELETED_EVENT;
import static com.redhat.service.bridge.manager.connectors.Events.CONNECTOR_KAFKA_TOPIC_CREATED_EVENT;
import static com.redhat.service.bridge.manager.connectors.Events.CONNECTOR_MANAGED_CONNECTOR_CREATED_EVENT;
import static com.redhat.service.bridge.manager.connectors.Events.KAFKA_TOPIC_DELETED_EVENT;

@ApplicationScoped
public class ConnectorsOrchestratorImpl implements ConnectorsOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsOrchestratorImpl.class);

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    EventBus eventBus;

    @Scheduled(every = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void schedule() {
        updatePendingConnectors().subscribe().with(messages -> {
            /* NOP */}, throwable -> LOGGER.error(throwable.getMessage()));
    }

    @Override
    @Transactional(Transactional.TxType.NEVER)
    public Uni<List<Message<ConnectorEntity>>> updatePendingConnectors() {
        // assume this class is not a singleton as we could have multiple pods running this
        List<ConnectorEntity> unprocessed = connectorsDAO.findUnprocessed();
        List<Uni<Message<ConnectorEntity>>> responses = new ArrayList<>();
        for (ConnectorEntity c : unprocessed) {
            responses.add(process(c));
        }
        // Uni.join().all(list must not be empty)
        if (responses.size() > 0) {
            return Uni.join().all(responses).andCollectFailures();
        } else {
            return Uni.createFrom().item(Collections.emptyList());
        }
    }

    private Uni<Message<ConnectorEntity>> process(ConnectorEntity c) {
        if (c.getDesiredStatus().equals(ConnectorStatus.READY) && c.getStatus() == ConnectorStatus.ACCEPTED) {
            return eventBus.request(CONNECTOR_ACCEPTED_EVENT, c);
        } else if (c.getDesiredStatus().equals(ConnectorStatus.READY) && c.getStatus() == ConnectorStatus.TOPIC_CREATED) {
            return eventBus.request(CONNECTOR_KAFKA_TOPIC_CREATED_EVENT, c);
        } else if (c.getDesiredStatus().equals(ConnectorStatus.READY) && c.getStatus() == ConnectorStatus.MANAGED_CONNECTOR_CREATED) {
            return eventBus.request(CONNECTOR_MANAGED_CONNECTOR_CREATED_EVENT, c);
        } else if (c.getDesiredStatus().equals(ConnectorStatus.READY) && c.getStatus() == ConnectorStatus.MANAGED_CONNECTOR_LOOKUP_FAILED) {
            return eventBus.request(CONNECTOR_MANAGED_CONNECTOR_CREATED_EVENT, c);
        } else if (c.getDesiredStatus().equals(ConnectorStatus.DELETED) && c.getStatus() == ConnectorStatus.READY) {
            return eventBus.request(CONNECTOR_DELETED_EVENT, c);
        } else if (c.getDesiredStatus().equals(ConnectorStatus.DELETED) && c.getStatus() == ConnectorStatus.TOPIC_DELETED) {
            return eventBus.request(KAFKA_TOPIC_DELETED_EVENT, c);
        }

        return Uni.createFrom().failure(new RuntimeException("Pending connector in a non processable state: " + c.getStatus()));
    }
}
