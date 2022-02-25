package com.redhat.service.bridge.manager.connectors;

import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.redhat.service.bridge.manager.models.ConnectorEntity;

public interface ConnectorsApiClient {

    Connector createConnector(ConnectorRequest connector);

    Connector getConnector(ConnectorEntity connectorEntity);

    void deleteConnector(String id);
}
