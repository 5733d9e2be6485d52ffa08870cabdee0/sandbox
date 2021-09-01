package com.redhat.developer.manager.api.user;

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
public class ConnectorsAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testGetEmptyConnectors() {
        List<ConnectorDTO> response = TestUtils.getConnectors().as(new TypeRef<List<ConnectorDTO>>() {
        });
        Assertions.assertEquals(0, response.size());
    }

    @Test
    public void createConnector() {
        TestUtils.createConnector(new ConnectorRequest("test"))
                .then().statusCode(200);
    }

    @Test
    public void testCreateAndGetConnector() {
        TestUtils.createConnector(new ConnectorRequest("test"))
                .then().statusCode(200);

        List<ConnectorDTO> response = TestUtils.getConnectors().as(new TypeRef<List<ConnectorDTO>>() {
        });

        Assertions.assertEquals(1, response.size());
        ConnectorDTO connector = response.get(0);
        Assertions.assertEquals("test", connector.getName());
        Assertions.assertEquals("jrota", connector.getCustomerId());
        Assertions.assertEquals(ConnectorStatus.REQUESTED, connector.getStatus());
        Assertions.assertNull(connector.getEndpoint());
    }

    @Test
    public void testAlreadyExistingConnector() {
        TestUtils.createConnector(new ConnectorRequest("test"))
                .then().statusCode(200);
        TestUtils.createConnector(new ConnectorRequest("test"))
                .then().statusCode(400);
    }
}
