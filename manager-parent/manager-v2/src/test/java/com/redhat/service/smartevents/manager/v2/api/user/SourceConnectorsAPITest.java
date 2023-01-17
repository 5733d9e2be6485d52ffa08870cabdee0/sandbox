package com.redhat.service.smartevents.manager.v2.api.user;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SourceConnectorResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
public class SourceConnectorsAPITest extends AbstractConnectorsAPITest {

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.SOURCE;
    }

    @Override
    protected Class<? extends ConnectorResponse> getResponseClass() {
        return SourceConnectorResponse.class;
    }

    @Override
    protected void additionalResponseAssertions(Response connectorResponse, ConnectorRequest connectorRequest) {
        // No additional checks for SourceConnectorResponse ATM
    }
}
