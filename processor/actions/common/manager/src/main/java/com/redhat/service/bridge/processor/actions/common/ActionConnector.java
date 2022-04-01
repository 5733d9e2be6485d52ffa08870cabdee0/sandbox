package com.redhat.service.bridge.processor.actions.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

public interface ActionConnector extends ActionAccepter {

    String getConnectorType();

    JsonNode connectorPayload(BaseAction action);

    String topicName(BaseAction action);
}
