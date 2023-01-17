package com.redhat.service.smartevents.manager.v2.api.user;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.api.APIConstants;
import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.TestUtils;

import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_TYPE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridgeReadyConditions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public abstract class AbstractConnectorsAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    JsonWebToken jwt;

    @Inject
    BridgeDAO bridgeDAO;

    protected abstract ConnectorType getConnectorType();

    protected abstract Class<? extends ConnectorResponse> getResponseClass();

    protected abstract void additionalResponseAssertions(Response response, ConnectorRequest connectorRequest);

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.SHARD_ID);
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.DEFAULT_ORGANISATION_ID);
        when(jwt.containsClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_USER_NAME);
        when(jwt.containsClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(true);
    }

    @Test
    public void testGetSourceConnectorsNoAuthentication() {
        TestUtils.listConnectors(getConnectorType()).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void createConnector() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ObjectNode connectorPayload = new ObjectNode(JsonNodeFactory.instance);
        connectorPayload.set("endpoint", JsonNodeFactory.instance.textNode("http://localhost:8080"));

        ConnectorRequest connectorRequest = new ConnectorRequest("myConnector");
        connectorRequest.setConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID);
        connectorRequest.setConnector(connectorPayload);

        Response response = TestUtils.addConnectorToBridge(bridgeResponse.getId(), connectorRequest, getConnectorType());
        assertThat(response.getStatusCode()).isEqualTo(202);

        ConnectorResponse connectorResponse = response.as(getResponseClass());
        assertThat(connectorResponse.getName()).isEqualTo("myConnector");
        assertThat(connectorResponse.getConnector().asText()).isEqualTo(connectorPayload.asText());
        assertThat(connectorResponse.getConnectorTypeId()).isEqualTo(DEFAULT_CONNECTOR_TYPE_ID);
        assertThat(connectorResponse.getStatus()).isEqualTo(ManagedResourceStatusV2.ACCEPTED);

        additionalResponseAssertions(response, connectorRequest);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addConnectorWithNullPayloadDefinitionToBridge() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        String requestBody = "{" +
                "\"name\": \"defaultconnector\"," +
                "\"connector_type_id\": \"mytype\"" +
                "}";

        Response response = TestUtils.addConnectorToBridgeWithRequestBody(bridgeResponse.getId(), requestBody, getConnectorType());
        assertThat(response.getStatusCode()).isEqualTo(400);

        ErrorsResponse errors = response.as(ErrorsResponse.class);
        assertThat(errors.getItems()).hasSize(1);

        ErrorResponse error = errors.getItems().get(0);
        assertThat(error.getId()).isEqualTo("1");
        assertThat(error.getCode()).isEqualTo("OPENBRIDGE-1");
        assertThat(error.getReason()).contains("Connector payload can't be null");
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void addConnectorWithNullType() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ObjectNode connectorPayload = new ObjectNode(JsonNodeFactory.instance);
        connectorPayload.set("endpoint", JsonNodeFactory.instance.textNode("http://localhost:8080"));

        ConnectorRequest connectorRequest = new ConnectorRequest("myConnector");
        connectorRequest.setConnector(connectorPayload);

        Response response = TestUtils.addConnectorToBridge(bridgeResponse.getId(), connectorRequest, getConnectorType());
        assertThat(response.getStatusCode()).isEqualTo(400);

        ErrorsResponse errors = response.as(ErrorsResponse.class);
        assertThat(errors.getItems()).hasSize(1);

        ErrorResponse error = errors.getItems().get(0);
        assertThat(error.getId()).isEqualTo("1");
        assertThat(error.getCode()).isEqualTo("OPENBRIDGE-1");
        assertThat(error.getReason()).contains("Connector type cannot be null or empty");
    }

    private BridgeResponse createBridge() {
        BridgeRequest r = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CLOUD_PROVIDER, TestConstants.DEFAULT_REGION);
        return TestUtils.createBridge(r).as(BridgeResponse.class);
    }

    protected BridgeResponse createAndDeployBridge() {
        BridgeResponse bridgeResponse = createBridge();
        setBridgeStatus(bridgeResponse.getId(), createBridgeReadyConditions());
        return bridgeResponse;
    }

    @Transactional
    protected void setBridgeStatus(String bridgeId, List<Condition> conditions) {
        Bridge bridge = bridgeDAO.findById(bridgeId);
        bridge.setConditions(conditions);
    }
}
