package com.redhat.service.smartevents.manager.v2.services;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ConnectorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ConnectorResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConnectorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.ACCEPTED;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.FAILED;
import static com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2.READY;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CLOUD_PROVIDER;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_TYPE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_ORGANISATION_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_REGION;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.services.ProcessorServiceImplTest.NOT_READY_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createBridge;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createFailedConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessor;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createReadyBridge;
import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getManagedResourceStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class AbstractConnectorServiceTest {

    public static final String NEW_CONNECTOR_NAME = "My Connector";
    public static final String NOT_READY_BRIDGE_ID = "not-ready-bridge-id";
    public static final String PROVISIONING_CONNECTOR_ID = "provisioning-connector-id";
    public static final String PROVISIONING_CONNECTOR_NAME = "provisioning-connector-name";
    public static final String FAILED_CONNECTOR_ID = "failed-connector-id";
    public static final String FAILED_CONNECTOR_NAME = "failed-connector-name";

    public static final String NON_EXISTING_BRIDGE_ID = "non-existing-bridge-id";
    public static final String NON_EXISTING_PROCESSOR_ID = "non-existing-processor-id";
    public static final String NON_EXISTING_CUSTOMER_ID = "non-existing-customer-id";

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    public abstract ConnectorService getConnectorService();

    public abstract ConnectorDAO getConnectorDAO();

    public abstract ConnectorType getConnectorType();


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

    @Test
    void testCreateProcessorWhenOrganizationHasNoQuota() {
        Bridge bridge = Fixtures.createReadyBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        ConnectorRequest request = new ConnectorRequest();
        request.setName(DEFAULT_CONNECTOR_NAME);
        assertThatExceptionOfType(NoQuotaAvailable.class)
                .isThrownBy(() -> getConnectorService().createConnector(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), bridge.getOrganisationId(), request));
    }

    @Test
    void testCreateProcessor() {
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
        assertThat(response.getConnector().asText()).isEqualTo(connectorPayload.asText());
    }
}
