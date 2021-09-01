package com.redhat.developer.manager;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.ConnectorStatus;
import com.redhat.developer.manager.models.Connector;
import com.redhat.developer.manager.requests.ConnectorRequest;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConnectorsServiceTest {

    @Inject
    ConnectorsService connectorsService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testGetEmptyConnectorsToDeploy() {
        List<Connector> connectors = connectorsService.getConnectorsToDeploy();
        Assertions.assertEquals(0, connectors.size());
    }

    @Test
    public void testGetEmptyConnectors() {
        List<Connector> connectors = connectorsService.getConnectors("jrota");
        Assertions.assertEquals(0, connectors.size());
    }

    @Test
    public void testGetConnectors() {
        ConnectorRequest request = new ConnectorRequest("test");
        connectorsService.createConnector("jrota", request);

        List<Connector> connectors = connectorsService.getConnectors("jrota");
        Assertions.assertEquals(1, connectors.size());

        // filter by customer id not implemented yet
        connectors = connectorsService.getConnectors("not-the-id");
        Assertions.assertEquals(1, connectors.size());
    }

    @Test
    public void testCreateConnector() {
        ConnectorRequest request = new ConnectorRequest("test");
        connectorsService.createConnector("jrota", request);

        List<Connector> connectors = connectorsService.getConnectorsToDeploy();
        Assertions.assertEquals(1, connectors.size());
        Assertions.assertEquals(ConnectorStatus.REQUESTED, connectors.get(0).getStatus());
        Assertions.assertNull(connectors.get(0).getEndpoint());

        connectors = connectorsService.getConnectors("jrota");
        Assertions.assertEquals(1, connectors.size());
    }

    @Test
    public void testUpdateConnectorStatus() {
        ConnectorRequest request = new ConnectorRequest("test");
        connectorsService.createConnector("jrota", request);

        List<Connector> connectors = connectorsService.getConnectorsToDeploy();
        Assertions.assertEquals(1, connectors.size());
        Assertions.assertEquals(ConnectorStatus.REQUESTED, connectors.get(0).getStatus());

        Connector connector = connectors.get(0);
        connector.setStatus(ConnectorStatus.PROVISIONING);
        connectorsService.updateConnector(connector);

        connectors = connectorsService.getConnectorsToDeploy();
        Assertions.assertEquals(0, connectors.size());

        connectors = connectorsService.getConnectors("jrota");
        Assertions.assertEquals(1, connectors.size());
        Assertions.assertEquals(ConnectorStatus.PROVISIONING, connectors.get(0).getStatus());
    }
}
