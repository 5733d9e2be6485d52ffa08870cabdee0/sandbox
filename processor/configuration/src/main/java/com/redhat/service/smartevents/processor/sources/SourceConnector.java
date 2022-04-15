package com.redhat.service.smartevents.processor.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.smartevents.infra.models.gateways.Source;

public interface SourceConnector extends SourceBean {

    String getConnectorType();

    JsonNode connectorPayload(Source source, String topicName);
}
