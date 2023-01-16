package com.redhat.service.smartevents.manager.v2.persistence.dao;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class SourceConnectorDAOTest extends AbstractConnectorDAOTest {

    @Inject
    SourceConnectorDAO sourceConnectorDAO;

    @Override
    public ConnectorDAO getConnectorDAO() {
        return sourceConnectorDAO;
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.SOURCE;
    }
}
