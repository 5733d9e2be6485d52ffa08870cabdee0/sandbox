package com.redhat.service.smartevents.manager.v2.api.user;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorListResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SinkConnectorListResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SinkConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.SinkConnectorDAO;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class SinkConnectorsAPITest extends AbstractConnectorsAPITest {

    @Inject
    SinkConnectorDAO sinkConnectorDAO;

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.SINK;
    }

    @Override
    protected ConnectorDAO getConnectorDAO() {
        return sinkConnectorDAO;
    }

    @Override
    protected Class<? extends ConnectorResponse> getResponseClass() {
        return SinkConnectorResponse.class;
    }

    @Override
    protected Class<? extends ConnectorListResponse> getListResponseClass() {
        return SinkConnectorListResponse.class;
    }

    @Override
    protected void additionalResponseAssertions(Response connectorResponse, ConnectorRequest connectorRequest) {
        SinkConnectorResponse sinkConnectorResponse = connectorResponse.as(SinkConnectorResponse.class);
        assertThat(sinkConnectorResponse.getUriDsl()).startsWith("knative").contains(sinkConnectorResponse.getId());
    }
}
