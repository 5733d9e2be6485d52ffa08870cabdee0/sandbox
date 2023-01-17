package com.redhat.service.smartevents.manager.v2.services;

import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;

public interface ConnectorService {
    Connector createConnector(String bridgeId, String customerId, String owner, String organisationId, ConnectorRequest connectorRequest);

    ConnectorResponse toResponse(Connector connector);
}
