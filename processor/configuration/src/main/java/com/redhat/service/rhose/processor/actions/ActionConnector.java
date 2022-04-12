package com.redhat.service.rhose.processor.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.rhose.infra.models.actions.BaseAction;

public interface ActionConnector extends ActionBean {

    String getConnectorType();

    JsonNode connectorPayload(BaseAction action, String topicName);
}
