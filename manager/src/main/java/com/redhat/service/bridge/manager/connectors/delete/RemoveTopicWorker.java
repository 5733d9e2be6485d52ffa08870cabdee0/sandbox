package com.redhat.service.bridge.manager.connectors.delete;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.connectors.AbstractConnectorWorker;
import com.redhat.service.bridge.manager.connectors.Events;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class RemoveTopicWorker extends AbstractConnectorWorker<ConnectorEntity> {

    @Inject
    RhoasService rhoasService;

    @ConsumeEvent(value = Events.CONNECTOR_DELETED_EVENT, blocking = true)
    public void consume(ConnectorEntity connectorEntity) {
        execute(connectorEntity);
    }

    @Override
    protected ConnectorEntity callExternalService(ConnectorEntity connectorEntity) {
        rhoasService
                .deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);
        return connectorEntity;
    }

    @Override
    protected ConnectorEntity updateEntityForSuccess(ConnectorEntity connectorEntity, ConnectorEntity serviceResponse) {
        connectorEntity.setStatus(ConnectorStatus.DELETED);
        return connectorEntity;
    }

    @Override
    protected ConnectorEntity updateEntityForError(ConnectorEntity connectorEntity, Throwable error) {
        connectorEntity.setStatus(ConnectorStatus.FAILED);
        return connectorEntity;
    }

    @Override
    protected void afterSuccessfullyUpdated(ConnectorEntity c) {
        eventBus.requestAndForget(Events.KAFKA_TOPIC_DELETED_EVENT, c);
    }
}
