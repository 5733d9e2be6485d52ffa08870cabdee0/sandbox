package com.redhat.service.bridge.processor.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

public interface ActionConnector extends ActionBean {

    String getConnectorType();

    JsonNode connectorPayload(BaseAction action, String topicName);
}
