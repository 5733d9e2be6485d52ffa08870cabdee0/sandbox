package com.redhat.service.smartevents.processor.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.smartevents.infra.models.actions.BaseAction;

public interface ActionConnector extends ActionBean {

    String getConnectorType();

    JsonNode connectorPayload(BaseAction action, String topicName);
}
