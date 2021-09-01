package com.redhat.developer.manager.dao;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.ConnectorStatus;
import com.redhat.developer.manager.TestConstants;
import com.redhat.developer.manager.models.Connector;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConnectorDAOTest {

    @Inject
    ConnectorDAO connectorDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testFindByStatus() {
        Connector connector = buildConnector();
        connectorDAO.persist(connector);

        List<Connector> retrievedConnectors = connectorDAO.findByStatus(ConnectorStatus.PROVISIONING);
        Assertions.assertEquals(0, retrievedConnectors.size());

        retrievedConnectors = connectorDAO.findByStatus(ConnectorStatus.AVAILABLE);
        Assertions.assertEquals(0, retrievedConnectors.size());

        retrievedConnectors = connectorDAO.findByStatus(ConnectorStatus.REQUESTED);
        Assertions.assertEquals(1, retrievedConnectors.size());
    }

    @Test
    public void testFindByNameAndCustomerId() {
        Connector connector = buildConnector();
        connectorDAO.persist(connector);

        Connector retrievedConnector = connectorDAO.findByNameAndCustomerId("not-the-id", TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertNull(retrievedConnector);

        retrievedConnector = connectorDAO.findByNameAndCustomerId(TestConstants.DEFAULT_CONNECTOR_NAME, "not-the-customer-id");
        Assertions.assertNull(retrievedConnector);

        retrievedConnector = connectorDAO.findByNameAndCustomerId(TestConstants.DEFAULT_CONNECTOR_NAME, TestConstants.DEFAULT_CUSTOMER_ID);
        Assertions.assertNotNull(retrievedConnector);
    }

    private Connector buildConnector() {
        Connector connector = new Connector();
        connector.setId("myId");
        connector.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        connector.setName(TestConstants.DEFAULT_CONNECTOR_NAME);
        connector.setStatus(ConnectorStatus.REQUESTED);

        return connector;
    }
}
