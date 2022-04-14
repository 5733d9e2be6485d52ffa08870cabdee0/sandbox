package com.redhat.service.smartevents.manager.connectors;

import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;

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

    void deleteConnector(String id);

}
