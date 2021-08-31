package com.redhat.developer.manager.api.internal;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.GenericType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.ConnectorDTO;
import com.redhat.developer.infra.dto.ConnectorStatusDTO;
import com.redhat.developer.manager.api.user.ConnectorsAPI;
import com.redhat.developer.manager.requests.ConnectorRequest;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ShardConnectorsSyncAPITest {

    @Inject
    ConnectorsAPI connectorsAPI;

    @Inject
    ShardConnectorsSyncAPI shardConnectorsSyncAPI;

    @Test
    @TestTransaction
    public void testGetEmptyConnectorsToDeploy() {
        List<ConnectorDTO> response = shardConnectorsSyncAPI.getConnectorsToDeploy().readEntity(new GenericType<List<ConnectorDTO>>() {
        });
        Assertions.assertEquals(0, response.size());
    }

    @Test
    @TestTransaction
    public void testGetConnectorsToDeploy() {
        connectorsAPI.createConnector(new ConnectorRequest("test")).getStatus();

        List<ConnectorDTO> response = shardConnectorsSyncAPI.getConnectorsToDeploy().readEntity(new GenericType<List<ConnectorDTO>>() {
        });

        Assertions.assertEquals(1, response.size());
        ConnectorDTO connector = response.get(0);
        Assertions.assertEquals("test", connector.getName());
        Assertions.assertEquals("jrota", connector.getCustomerId());
        Assertions.assertEquals(ConnectorStatusDTO.REQUESTED, connector.getStatus());
        Assertions.assertNull(connector.getEndpoint());
    }

    @Test
    @TestTransaction
    public void testNotifyDeployment() {
        connectorsAPI.createConnector(new ConnectorRequest("test")).getStatus();

        List<ConnectorDTO> connectorsToDeploy = shardConnectorsSyncAPI.getConnectorsToDeploy().readEntity(new GenericType<List<ConnectorDTO>>() {
        });

        Assertions.assertEquals(1, connectorsToDeploy.size());

        ConnectorDTO connector = connectorsToDeploy.get(0);
        connector.setStatus(ConnectorStatusDTO.PROVISIONING);
        shardConnectorsSyncAPI.notifyDeployment(connector);

        connectorsToDeploy = shardConnectorsSyncAPI.getConnectorsToDeploy().readEntity(new GenericType<List<ConnectorDTO>>() {
        });

        Assertions.assertEquals(0, connectorsToDeploy.size());
    }
}
