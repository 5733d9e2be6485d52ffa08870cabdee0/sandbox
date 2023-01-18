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
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorListResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.TestUtils;

import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.ACCEPTED;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.READY;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_TYPE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridgeReadyConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createConnectorAcceptedConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createConnectorReadyConditions;
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

    protected abstract ConnectorDAO getConnectorDAO();

    protected abstract Class<? extends ConnectorResponse> getResponseClass();

    protected abstract Class<? extends ConnectorListResponse> getListResponseClass();

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
        TestUtils.listConnectors(TestConstants.DEFAULT_BRIDGE_ID, getConnectorType()).then().statusCode(401);
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

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listConnectors() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ConnectorResponse p1 = TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector"), getConnectorType()).as(getResponseClass());
        ConnectorResponse p2 = TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector2"), getConnectorType()).as(getResponseClass());

        ConnectorListResponse<?> listResponse = TestUtils.listConnectors(bridgeResponse.getId(), getConnectorType(), 0, 100).as(getListResponseClass());
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(2L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default. The first created is the last to be listed.
        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
        assertThat(listResponse.getItems().get(1).getId()).isEqualTo(p1.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listConnectorsWhenFilterByName() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector"), getConnectorType()).as(getResponseClass());
        ConnectorResponse p2 = TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector2"), getConnectorType()).as(getResponseClass());

        ConnectorListResponse<?> listResponse = TestUtils.listConnectorsFilterByName(bridgeResponse.getId(), "myConnector2", getConnectorType()).as(getListResponseClass());
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(1L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listConnectorsWhenFilterByStatus() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ConnectorResponse p1 = TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector"), getConnectorType()).as(getResponseClass());
        ConnectorResponse p2 = TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector2"), getConnectorType()).as(getResponseClass());

        setConnectorStatus(p1.getId(), createConnectorAcceptedConditions());
        setConnectorStatus(p2.getId(), createConnectorReadyConditions());

        ConnectorListResponse<?> listResponse = TestUtils.listConnectorsFilterByStatus(bridgeResponse.getId(), getConnectorType(), READY).as(getListResponseClass());
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(1L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listConnectorsWhenFilterByMultipleStatuses() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ConnectorResponse p1 = TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector"), getConnectorType()).as(getResponseClass());
        ConnectorResponse p2 = TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector2"), getConnectorType()).as(getResponseClass());

        setConnectorStatus(p1.getId(), createConnectorAcceptedConditions());
        setConnectorStatus(p2.getId(), createConnectorReadyConditions());

        ConnectorListResponse<?> listResponse = TestUtils.listConnectorsFilterByStatus(bridgeResponse.getId(), getConnectorType(), ACCEPTED, READY).as(getListResponseClass());
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(2L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        listResponse.getItems().forEach((i) -> assertThat(i.getId()).isIn(p1.getId(), p2.getId()));
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listConnectorsWhenFilterByStatusWithIncorrectValue() {
        // See JAX-RS 2.1 Specification, Section 3.2.
        // HTTP-404 is correct if the QueryString contains an invalid value.
        // If the field or property is annotated with @MatrixParam, @QueryParam or @PathParam then an implementation
        // MUST generate an instance of NotFoundException (404 status) that wraps the thrown exception...
        BridgeResponse bridgeResponse = createAndDeployBridge();

        TestUtils.listConnectorsFilterByStatusWithAnyValue(bridgeResponse.getId(), getConnectorType(), "banana").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listConnectorsWhenFilterByNameAndStatus() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ConnectorResponse p1 = TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector"), getConnectorType()).as(getResponseClass());
        ConnectorResponse p2 = TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector2"), getConnectorType()).as(getResponseClass());

        setConnectorStatus(p1.getId(), createConnectorAcceptedConditions());
        setConnectorStatus(p2.getId(), createConnectorReadyConditions());

        ConnectorListResponse<?> listResponse = TestUtils.listConnectorsFilterByNameAndStatus(bridgeResponse.getId(), "myConnector", getConnectorType(), READY).as(getListResponseClass());
        assertThat(listResponse.getPage()).isZero();
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(1L);

        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p2.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listConnectorsWhenPageOffsetIsSpecified() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ConnectorResponse p1 = TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector"), getConnectorType()).as(getResponseClass());
        TestUtils.addConnectorToBridge(bridgeResponse.getId(), buildDefaultConnectorRequest("myConnector2"), getConnectorType()).as(getResponseClass());

        ConnectorListResponse<?> listResponse = TestUtils.listConnectors(bridgeResponse.getId(), getConnectorType(), 1, 1).as(getListResponseClass());
        assertThat(listResponse.getPage()).isEqualTo(1L);
        assertThat(listResponse.getSize()).isEqualTo(1L);
        assertThat(listResponse.getTotal()).isEqualTo(2L);

        // Results are sorted descending by default. The last page, 1, will contain the first processor.
        assertThat(listResponse.getItems().get(0).getId()).isEqualTo(p1.getId());
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listConnectorsWhenBridgeDoesNotExist() {
        assertThat(TestUtils.listConnectors("doesNotExist", getConnectorType(), 0, 100).getStatusCode()).isEqualTo(404);
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

    @Transactional
    protected void setConnectorStatus(String connectorId, List<Condition> conditions) {
        Connector connector = getConnectorDAO().findById(connectorId);
        connector.setConditions(conditions);
    }

    private ConnectorRequest buildDefaultConnectorRequest(String connectorName) {
        ConnectorRequest request = new ConnectorRequest(connectorName);
        request.setConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID);
        request.setConnector(JsonNodeFactory.instance.objectNode());
        return request;
    }
}
