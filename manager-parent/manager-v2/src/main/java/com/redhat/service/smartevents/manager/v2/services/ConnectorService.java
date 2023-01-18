package com.redhat.service.smartevents.manager.v2.services;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.v2.api.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;

public interface ConnectorService<T extends ConnectorResponse> {

    ListResult<Connector> getConnectors(String bridgeId, String customerId, QueryResourceInfo queryInfo);

    Connector createConnector(String bridgeId, String customerId, String owner, String organisationId, ConnectorRequest connectorRequest);

    T toResponse(Connector connector);
}
