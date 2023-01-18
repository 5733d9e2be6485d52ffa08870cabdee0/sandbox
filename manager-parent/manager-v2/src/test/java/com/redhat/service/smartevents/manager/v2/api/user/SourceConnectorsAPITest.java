package com.redhat.service.smartevents.manager.v2.api.user;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorListResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SourceConnectorListResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SourceConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.SourceConnectorDAO;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
public class SourceConnectorsAPITest extends AbstractConnectorsAPITest {

    @Inject
    SourceConnectorDAO sourceConnectorDAO;

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.SOURCE;
    }

    @Override
    protected ConnectorDAO getConnectorDAO() {
        return sourceConnectorDAO;
    }

    @Override
    protected Class<? extends ConnectorResponse> getResponseClass() {
        return SourceConnectorResponse.class;
    }

    @Override
    protected Class<? extends ConnectorListResponse> getListResponseClass() {
        return SourceConnectorListResponse.class;
    }

    @Override
    protected void additionalResponseAssertions(Response connectorResponse, ConnectorRequest connectorRequest) {
        // No additional checks for SourceConnectorResponse ATM
    }
}
