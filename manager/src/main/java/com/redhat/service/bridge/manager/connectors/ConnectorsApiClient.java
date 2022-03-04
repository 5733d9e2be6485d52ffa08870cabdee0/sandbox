package com.redhat.service.bridge.manager.connectors;

import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.redhat.service.bridge.manager.models.ConnectorEntity;

public interface ConnectorsApiClient {

    /**
     * Retrieve the Managed Connector represented by the ConnectorEntity.
     *
     * @param connectorEntity
     * @return The Managed Connector or null if not found.
     */
    Connector getConnector(ConnectorEntity connectorEntity);

    Connector createConnector(ConnectorRequest connectorRequest);

    Connector createConnector(ConnectorEntity connectorEntity);

    void deleteConnector(String id);

}
