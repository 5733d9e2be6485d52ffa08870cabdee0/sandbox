package com.redhat.service.smartevents.manager.connectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.smartevents.manager.models.Processor;

public interface ConnectorsService {

    void createConnectorEntity(Processor processor);

    JsonNode createConnectorDefinition(String connectorId);

    void deleteConnectorEntity(Processor processor);
}
