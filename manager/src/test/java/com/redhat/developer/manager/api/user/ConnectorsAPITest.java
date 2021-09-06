package com.redhat.developer.manager.api.user;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.ConnectorStatus;
import com.redhat.developer.manager.TestConstants;
import com.redhat.developer.manager.api.models.requests.ConnectorRequest;
import com.redhat.developer.manager.api.models.responses.ConnectorListResponse;
import com.redhat.developer.manager.api.models.responses.ConnectorResponse;
import com.redhat.developer.manager.utils.DatabaseManagerUtils;
import com.redhat.developer.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;

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
        ConnectorListResponse response = TestUtils.getConnectors().as(ConnectorListResponse.class);
        Assertions.assertEquals(0, response.getItems().size());
    }

    @Test
    public void createConnector() {
        TestUtils.createConnector(new ConnectorRequest(TestConstants.DEFAULT_CONNECTOR_NAME))
                .then().statusCode(200);
    }

    @Test
    public void testCreateAndGetConnector() {
        TestUtils.createConnector(new ConnectorRequest(TestConstants.DEFAULT_CONNECTOR_NAME))
                .then().statusCode(200);

        ConnectorListResponse response = TestUtils.getConnectors().as(ConnectorListResponse.class);

        Assertions.assertEquals(1, response.getItems().size());
        ConnectorResponse connector = response.getItems().get(0);
        Assertions.assertEquals(TestConstants.DEFAULT_CONNECTOR_NAME, connector.getName());
        Assertions.assertEquals(ConnectorStatus.REQUESTED, connector.getStatus());
        Assertions.assertNotNull(connector.getSubmittedAt());

        Assertions.assertNull(connector.getEndpoint());
    }

    @Test
    public void testAlreadyExistingConnector() {
        TestUtils.createConnector(new ConnectorRequest(TestConstants.DEFAULT_CONNECTOR_NAME))
                .then().statusCode(200);
        TestUtils.createConnector(new ConnectorRequest(TestConstants.DEFAULT_CONNECTOR_NAME))
                .then().statusCode(400);
    }
}
