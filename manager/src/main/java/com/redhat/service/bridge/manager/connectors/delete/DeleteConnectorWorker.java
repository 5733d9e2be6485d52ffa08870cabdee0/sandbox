package com.redhat.service.bridge.manager.connectors.delete;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.manager.connectors.AbstractConnectorWorker;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.connectors.Events;
import com.redhat.service.bridge.manager.models.ConnectorEntity;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class DeleteConnectorWorker extends AbstractConnectorWorker<ConnectorEntity> {

    @Inject
    ConnectorsApiClient connectorsApiClient;

    @ConsumeEvent(value = Events.KAFKA_TOPIC_DELETED_EVENT, blocking = true)
    public void consume(ConnectorEntity connectorEntity) {
        execute(connectorEntity);
    }

    @Override
    protected boolean shouldDeleteAfterSuccess() {
        return true;
    }

    @Override
    protected boolean shouldDeleteAfterFailure() {
        return true;
    }

    @Override
    protected ConnectorEntity callExternalService(ConnectorEntity connectorEntity) {
        connectorsApiClient
                .deleteConnector(connectorEntity.getConnectorExternalId());
        return connectorEntity;
    }

    @Override
    protected ConnectorEntity updateEntityForError(ConnectorEntity connectorEntity, Throwable error) {
        connectorEntity.setStatus(ConnectorStatus.FAILED);
        return connectorEntity;
    }
}
