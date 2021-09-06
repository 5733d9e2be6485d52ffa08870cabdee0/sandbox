package com.redhat.developer.manager;

import java.util.List;

import com.redhat.developer.manager.api.models.requests.ConnectorRequest;
import com.redhat.developer.manager.models.Connector;

public interface ConnectorsService {

    Connector createConnector(String customerId, ConnectorRequest connectorRequest);

    List<Connector> getConnectors(String customerId);

    List<Connector> getConnectorsToDeploy();

    Connector updateConnector(Connector connector);
}
