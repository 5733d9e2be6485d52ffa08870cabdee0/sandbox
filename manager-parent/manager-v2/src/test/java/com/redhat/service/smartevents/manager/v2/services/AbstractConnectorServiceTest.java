package com.redhat.service.smartevents.manager.v2.services;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.ACCEPTED;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_TYPE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_ORGANISATION_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridgeDeletingConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridgeDeprovisionConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridgePreparingConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridgeProvisionConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createReadyBridge;
import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getManagedResourceStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public abstract class AbstractConnectorServiceTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    protected abstract ConnectorService getConnectorService();

    protected abstract ConnectorDAO getConnectorDAO();

    protected abstract ConnectorType getConnectorType();

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    void testCreateConnectorWhenBridgeNotActive() {
        Bridge bridge = Fixtures.createAcceptedBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        ConnectorRequest request = new ConnectorRequest();
        request.setName(DEFAULT_CONNECTOR_NAME);
        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> getConnectorService().createConnector(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), bridge.getOrganisationId(), request));
    }

    @Test
    void testCreateConnectorWhenBridgeDoesNotExist() {
        ConnectorRequest request = new ConnectorRequest();
        request.setName(DEFAULT_CONNECTOR_NAME);
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> getConnectorService().createConnector("id that does not exist", DEFAULT_CUSTOMER_ID, DEFAULT_USER_NAME, DEFAULT_ORGANISATION_ID, request));
    }

    @Test
    void testCreateConnectorWhenConnectorWithSameNameAlreadyExists() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Connector connector = Fixtures.createConnector(bridge, getConnectorType());
        getConnectorDAO().persist(connector);

        ConnectorRequest request = new ConnectorRequest();
        request.setName(DEFAULT_CONNECTOR_NAME);

        assertThatExceptionOfType(AlreadyExistingItemException.class)
                .isThrownBy(() -> getConnectorService().createConnector(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), bridge.getOrganisationId(), request));
    }

    // See the quota configuration settings in application.properties
    @Test
    void testCreateConnectorWhenOrganizationHasNoQuota() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridge.setOrganisationId("organisation_with_1_quota");
        bridgeDAO.persist(bridge);

        // Consume one quota
        ConnectorRequest request = new ConnectorRequest();
        request.setName(DEFAULT_CONNECTOR_NAME + "1");
        request.setConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID);
        request.setConnector(JsonNodeFactory.instance.objectNode());

        getConnectorService().createConnector(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), bridge.getOrganisationId(), request);

        // The organisation has only one quota, should raise an exception.
        request.setName(DEFAULT_CONNECTOR_NAME + "2");
        assertThatExceptionOfType(NoQuotaAvailable.class)
                .isThrownBy(() -> getConnectorService().createConnector(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), "organisation_with_no_quota", request));
    }

    @Test
    void testCreateConnector() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        ObjectNode connectorPayload = new ObjectNode(JsonNodeFactory.instance);
        connectorPayload.set("endpoint", JsonNodeFactory.instance.textNode("http://localhost:8080"));

        ConnectorRequest request = new ConnectorRequest();
        request.setName(DEFAULT_CONNECTOR_NAME);
        request.setConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID);
        request.setConnector(connectorPayload);

        Connector connector = getConnectorService().createConnector(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), bridge.getOrganisationId(), request);
        assertThat(connector.getId()).isNotNull();
        assertThat(connector.getOwner()).isEqualTo(bridge.getOwner());
        assertThat(connector.getSubmittedAt()).isNotNull();
        assertThat(connector.getGeneration()).isEqualTo(0);
        assertThat(connector.getConnectorTypeId()).isEqualTo(request.getConnectorTypeId());
        assertThat(connector.getType()).isEqualTo(getConnectorType());
        assertThat(connector.getTopicName()).isNull();
        assertThat(connector.getError()).isNull();
        assertThat(connector.getName()).isEqualTo(request.getName());
        assertThat(connector.getPublishedAt()).isNull();
        assertThat(connector.getConnectorExternalId()).isNull();
        assertThat(connector.getBridge()).isNotNull();
        assertThat(connector.getConditions()).hasSizeGreaterThan(0);
        assertThat(connector.getDefinition()).isNotNull();
        assertThat(connector.getDefinition().getConnector().asText()).isEqualTo(connectorPayload.asText());

        assertThat(connector.getOperation().getType()).isEqualTo(OperationType.CREATE);
        assertThat(connector.getOperation().getRequestedAt()).isNotNull();
        assertThat(connector.getOperation().getCompletedAt()).isNull();
        assertThat(StatusUtilities.getManagedResourceStatus(connector)).isEqualTo(ManagedResourceStatusV2.ACCEPTED);
    }

    @Test
    void testGetConnector() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Connector connector = Fixtures.createReadyConnector(bridge, getConnectorType());
        getConnectorDAO().persist(connector);

        Connector found = getConnectorService().getConnector(DEFAULT_BRIDGE_ID, DEFAULT_CONNECTOR_ID, DEFAULT_CUSTOMER_ID);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(DEFAULT_CONNECTOR_ID);
        assertThat(found.getBridge().getId()).isEqualTo(DEFAULT_BRIDGE_ID);
        assertThat(found.getBridge().getCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
    }

    @Test
    void testGetConnectorWhenBridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> getConnectorService().getConnector("does not exist", DEFAULT_CONNECTOR_ID, DEFAULT_CUSTOMER_ID));
    }

    @Test
    void testGetConnectorWhenDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> getConnectorService().getConnector(DEFAULT_BRIDGE_ID, "does not exist", DEFAULT_CUSTOMER_ID));
    }

    @Test
    void testGetConnectorWhenCustomerDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> getConnectorService().getConnector(DEFAULT_BRIDGE_ID, DEFAULT_PROCESSOR_ID, "does not exist"));
    }

    @Test
    void testGetConnectors() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        // Store 3 connectors
        ConnectorRequest request = new ConnectorRequest();
        request.setName(DEFAULT_CONNECTOR_NAME + "1");
        request.setConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID);
        request.setConnector(JsonNodeFactory.instance.objectNode());
        getConnectorService().createConnector(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), bridge.getOrganisationId(), request);

        request.setName(DEFAULT_CONNECTOR_NAME + "2");
        getConnectorService().createConnector(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), bridge.getOrganisationId(), request);

        request.setName(DEFAULT_CONNECTOR_NAME + "3");
        getConnectorService().createConnector(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), bridge.getOrganisationId(), request);

        ListResult<Connector> results = getConnectorService().getConnectors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, new QueryResourceInfo(0, 100));
        assertThat(results).isNotNull();
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isEqualTo(3L);
        assertThat(results.getTotal()).isEqualTo(3L);
        assertThat(results.getItems().get(0).getName()).isEqualTo(DEFAULT_CONNECTOR_NAME + "3");
        assertThat(results.getItems().get(1).getName()).isEqualTo(DEFAULT_CONNECTOR_NAME + "2");
        assertThat(results.getItems().get(2).getName()).isEqualTo(DEFAULT_CONNECTOR_NAME + "1");
    }

    @Test
    void testGetConnectorsWhenNoConnectorsOnBridge() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        ListResult<Processor> results = getConnectorService().getConnectors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, new QueryResourceInfo(0, 100));
        assertThat(results).isNotNull();
        assertThat(results.getPage()).isZero();
        assertThat(results.getSize()).isZero();
        assertThat(results.getTotal()).isZero();
    }

    @Test
    void testGetConnectorsWhenBridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> getConnectorService().getConnectors("UNEXISTING BRIDGE", DEFAULT_CUSTOMER_ID, new QueryResourceInfo(0, 100)));
    }

    @ParameterizedTest
    @MethodSource("getConnectorsWhenBridgeHasConditions")
    void testGetConnectorsWhenBridgeHasConditions(List<Condition> conditions) {
        Bridge bridge = createBridge();
        bridge.setConditions(conditions);
        bridgeDAO.persist(bridge);

        assertThatExceptionOfType(BridgeLifecycleException.class)
                .isThrownBy(() -> getConnectorService().getConnectors(DEFAULT_BRIDGE_ID, DEFAULT_CUSTOMER_ID, new QueryResourceInfo(0, 100)));
    }

    private static Stream<Arguments> getConnectorsWhenBridgeHasConditions() {
        Object[][] arguments = {
                { createBridgePreparingConditions() },
                { createBridgeProvisionConditions() },
                { createBridgeDeprovisionConditions() },
                { createBridgeDeletingConditions() }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

    @Test
    public void testToResponse() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        ObjectNode connectorPayload = new ObjectNode(JsonNodeFactory.instance);
        connectorPayload.set("endpoint", JsonNodeFactory.instance.textNode("http://localhost:8080"));

        ConnectorRequest request = new ConnectorRequest();
        request.setName(DEFAULT_CONNECTOR_NAME);
        request.setConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID);
        request.setConnector(connectorPayload);

        Connector connector = getConnectorService().createConnector(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), bridge.getOrganisationId(), request);
        ConnectorResponse response = getConnectorService().toResponse(connector);

        assertThat(response.getId()).isEqualTo(connector.getId());
        assertThat(response.getName()).isEqualTo(connector.getName());
        assertThat(response.getStatus()).isEqualTo(ACCEPTED);
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getSubmittedAt()).isEqualTo(connector.getSubmittedAt());
        assertThat(response.getModifiedAt()).isNull();
        assertThat(response.getOwner()).isEqualTo(bridge.getOwner());
        assertThat(response.getHref()).contains(V2APIConstants.V2_USER_API_BASE_PATH, bridge.getId());
        assertThat(response.getStatusMessage()).isNull();
        assertThat(response.getConnector().asText()).isEqualTo(connector.getDefinition().getConnector().asText());

        additionalResponseAssertions(response, bridge, connector);
    }

    protected abstract void additionalResponseAssertions(ConnectorResponse connectorResponse, Bridge bridge, Connector connector);

}
