package com.redhat.service.smartevents.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;

public interface GatewayConnector<T extends Gateway> extends GatewayBean {

    ConnectorType getConnectorType();

    String getConnectorTypeId();

    JsonNode connectorPayload(T gateway, String topicName);

    JsonNode connectorPayload(T gateway, String topicName, String errorHandlerTopicName);

}
