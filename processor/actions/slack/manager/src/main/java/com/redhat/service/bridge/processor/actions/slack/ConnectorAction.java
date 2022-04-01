package com.redhat.service.bridge.processor.actions.slack;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

public interface ConnectorAction extends ActionProvider {

    String getConnectorType();

    JsonNode connectorPayload(BaseAction action);

    String topicName(BaseAction action);

    @Override
    default boolean isConnectorAction() {
        return true;
    }
}
