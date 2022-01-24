package com.redhat.service.bridge.manager.connectors;

import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;

public interface ConnectorsApiClient {

    Connector createConnector(ConnectorRequest connector);

    void deleteConnector(String id, String kafkaId);
}
