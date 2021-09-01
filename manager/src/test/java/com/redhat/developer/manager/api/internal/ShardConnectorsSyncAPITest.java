package com.redhat.developer.manager.api.internal;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.ConnectorDTO;
import com.redhat.developer.infra.dto.ConnectorStatus;
import com.redhat.developer.manager.requests.ConnectorRequest;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;
import com.redhat.developer.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class ShardConnectorsSyncAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testGetEmptyConnectorsToDeploy() {
        List<ConnectorDTO> response = TestUtils.getConnectorsToDeploy().as(new TypeRef<List<ConnectorDTO>>() {
        });
        Assertions.assertEquals(0, response.size());
    }

    @Test
    public void testGetConnectorsToDeploy() {
        TestUtils.createConnector(new ConnectorRequest("test"));

        List<ConnectorDTO> response = TestUtils.getConnectorsToDeploy().as(new TypeRef<List<ConnectorDTO>>() {
        });

        Assertions.assertEquals(1, response.size());
        ConnectorDTO connector = response.get(0);
        Assertions.assertEquals("test", connector.getName());
        Assertions.assertEquals("jrota", connector.getCustomerId());
        Assertions.assertEquals(ConnectorStatus.REQUESTED, connector.getStatus());
        Assertions.assertNull(connector.getEndpoint());
    }

    @Test
    public void testNotifyDeployment() {
        TestUtils.createConnector(new ConnectorRequest("test"));

        List<ConnectorDTO> connectorsToDeploy = TestUtils.getConnectorsToDeploy().as(new TypeRef<List<ConnectorDTO>>() {
        });

        Assertions.assertEquals(1, connectorsToDeploy.size());

        ConnectorDTO connector = connectorsToDeploy.get(0);
        connector.setStatus(ConnectorStatus.PROVISIONING);
        TestUtils.updateConnector(connector).then().statusCode(200);

        connectorsToDeploy = TestUtils.getConnectorsToDeploy().as(new TypeRef<List<ConnectorDTO>>() {
        });

        Assertions.assertEquals(0, connectorsToDeploy.size());
    }
}
