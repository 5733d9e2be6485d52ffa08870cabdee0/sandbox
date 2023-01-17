package com.redhat.service.smartevents.manager.v2.api.user;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SinkConnectorResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class SinkConnectorsAPITest extends AbstractConnectorsAPITest {

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.SINK;
    }

    @Override
    protected Class<? extends ConnectorResponse> getResponseClass() {
        return SinkConnectorResponse.class;
    }

    @Override
    protected void additionalResponseAssertions(Response connectorResponse, ConnectorRequest connectorRequest) {
        SinkConnectorResponse sinkConnectorResponse = connectorResponse.as(SinkConnectorResponse.class);
        assertThat(sinkConnectorResponse.getUriDsl()).startsWith("knative").contains(sinkConnectorResponse.getId());
    }
}
