package com.redhat.service.smartevents.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;

public interface GatewayConnector<T extends Gateway> extends GatewayBean<T> {

    String getConnectorType();

    JsonNode connectorPayload(T gateway, String topicName);
}
