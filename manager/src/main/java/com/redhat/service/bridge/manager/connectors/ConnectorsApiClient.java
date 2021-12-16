package com.redhat.service.bridge.manager.connectors;

import com.openshift.cloud.api.connector.models.Connector;

public interface ConnectorsApiClient {

    Connector createConnector(Connector connector);
}
