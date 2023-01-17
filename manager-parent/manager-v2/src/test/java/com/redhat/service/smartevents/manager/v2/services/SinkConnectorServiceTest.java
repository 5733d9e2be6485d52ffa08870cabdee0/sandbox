package com.redhat.service.smartevents.manager.v2.services;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.SinkConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.SinkConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class SinkConnectorServiceTest extends AbstractConnectorServiceTest {

    @Inject
    SinkConnectorService sinkConnectorService;

    @Inject
    SinkConnectorDAO sinkConnectorDAO;

    @Override
    public ConnectorService getConnectorService() {
        return sinkConnectorService;
    }

    @Override
    public ConnectorDAO getConnectorDAO() {
        return sinkConnectorDAO;
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.SINK;
    }

    @Override
    public void additionalResponseAssertions(ConnectorResponse connectorResponse, Bridge bridge, Connector connector) {
        assertThat(connectorResponse).isInstanceOf(SinkConnectorResponse.class);
        assertThat(((SinkConnectorResponse) connectorResponse).getUriDsl()).startsWith("knative").contains(connectorResponse.getId());
    }
}
