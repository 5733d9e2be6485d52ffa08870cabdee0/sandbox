package com.redhat.service.smartevents.manager.v2.services;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.SourceConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class SourceConnectorServiceTest extends AbstractConnectorServiceTest {

    @Inject
    SourceConnectorServiceImpl sourceConnectorService;

    @Inject
    SourceConnectorDAO sourceConnectorDAO;

    @Override
    public ConnectorService getConnectorService() {
        return sourceConnectorService;
    }

    @Override
    public ConnectorDAO getConnectorDAO() {
        return sourceConnectorDAO;
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.SOURCE;
    }

    @Override
    public void additionalResponseAssertions(ConnectorResponse connectorResponse, Bridge bridge, Connector connector) {
        // No additional checks for SourceConnectorResponse ATM
    }
}
