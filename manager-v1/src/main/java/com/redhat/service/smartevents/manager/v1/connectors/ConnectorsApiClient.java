package com.redhat.service.smartevents.manager.v1.connectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.redhat.service.smartevents.manager.v1.persistence.models.ConnectorEntity;

public interface ConnectorsApiClient {

    /**
     * Retrieve the Managed Connector represented by the ConnectorEntity.
     *
     * @param connectorExternalId The ID of the Managed Connector
     * @return The Managed Connector or null if not found.
     */
    Connector getConnector(String connectorExternalId);

    Connector createConnector(ConnectorRequest connectorRequest);

    Connector createConnector(ConnectorEntity connectorEntity);

    Connector updateConnector(String connectorExternalId, JsonNode definition);

    void deleteConnector(String id);

}
