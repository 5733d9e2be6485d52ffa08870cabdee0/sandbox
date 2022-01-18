package com.redhat.service.bridge.manager.connectors.creation;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.connectors.AbstractConnectorWorker;
import com.redhat.service.bridge.manager.connectors.Events;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class CreateTopicWorker extends AbstractConnectorWorker<Topic> {

    @Inject
    RhoasService rhoasService;

    @ConsumeEvent(value = Events.CONNECTOR_CREATED_EVENT, blocking = true)
    public void consume(ConnectorEntity connectorEntity) {
        execute(connectorEntity);
    }

    @Override
    protected Topic callExternalService(ConnectorEntity connectorEntity) {
        String topicName = connectorEntity.getTopicName();
        return rhoasService.createTopicAndGrantAccessFor(topicName, RhoasTopicAccessType.PRODUCER);
    }

    @Override
    protected ConnectorEntity updateEntityForSuccess(ConnectorEntity connectorEntity, Topic serviceResponse) {
        connectorEntity.setStatus(ConnectorStatus.TOPIC_CREATED);
        return connectorEntity;
    }

    @Override
    protected ConnectorEntity updateEntityForError(ConnectorEntity connectorEntity, Throwable error) {
        connectorEntity.setStatus(ConnectorStatus.FAILED);
        return connectorEntity;
    }

    @Override
    protected void afterSuccessfullyUpdated(ConnectorEntity c) {
        eventBus.requestAndForget(Events.KAFKA_TOPIC_CREATED_EVENT, c);
    }
}
