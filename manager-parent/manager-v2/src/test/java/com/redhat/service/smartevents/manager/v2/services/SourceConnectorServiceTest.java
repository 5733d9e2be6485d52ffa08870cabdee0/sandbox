package com.redhat.service.smartevents.manager.v2.services;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.manager.v2.persistence.dao.AbstractConnectorDAOTest;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.SourceConnectorDAO;
import com.redhat.service.smartevents.test.resource.PostgresResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class SourceConnectorServiceTest extends AbstractConnectorServiceTest {

    @Inject
    SourceConnectorService sourceConnectorService;

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
}
