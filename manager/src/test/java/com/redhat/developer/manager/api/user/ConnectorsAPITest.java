package com.redhat.developer.manager.api.user;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.GenericType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.ConnectorDTO;
import com.redhat.developer.infra.dto.ConnectorStatusDTO;
import com.redhat.developer.manager.requests.ConnectorRequest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConnectorsAPITest {

    @Inject
    ConnectorsAPI connectorsAPI;

    @Test
    @TestTransaction
    public void testGetEmptyConnectors() {
        List<ConnectorDTO> response = connectorsAPI.getConnectors().readEntity(new GenericType<List<ConnectorDTO>>() {
        });
        Assertions.assertEquals(0, response.size());
    }

    @Test
    @TestTransaction
    public void createConnector() {
        int statusCode = connectorsAPI.createConnector(new ConnectorRequest("test")).getStatus();

        Assertions.assertEquals(HttpResponseStatus.OK.code(), statusCode);
    }

    @Test
    @TestTransaction
    public void testCreateAndGetConnector() {
        connectorsAPI.createConnector(new ConnectorRequest("test")).getStatus();
        List<ConnectorDTO> response = connectorsAPI.getConnectors().readEntity(new GenericType<List<ConnectorDTO>>() {
        });

        Assertions.assertEquals(1, response.size());
        ConnectorDTO connector = response.get(0);
        Assertions.assertEquals("test", connector.getName());
        Assertions.assertEquals("jrota", connector.getCustomerId());
        Assertions.assertEquals(ConnectorStatusDTO.REQUESTED, connector.getStatus());
        Assertions.assertNull(connector.getEndpoint());
    }
}
