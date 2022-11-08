package com.redhat.service.smartevents.manager.v1.connectors;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.openshift.cloud.api.connector.ConnectorsApi;
import com.openshift.cloud.api.connector.invoker.ApiException;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.openshift.cloud.api.connector.models.Error;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ConnectorCreationException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ConnectorDeletionException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ConnectorGetException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ConnectorUpdateException;
import com.redhat.service.smartevents.infra.v1.api.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.v1.persistence.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.v1.persistence.models.Processor;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ConnectorsApiClientTest {

    private static final String TEST_CONNECTOR_ID = "test-connector-id";
    private static final String TEST_CONNECTOR_NAME = "test-connector-name";
    private static final String TEST_CONNECTOR_TYPE_ID = "test-connector-type";
    private static final String TEST_CONNECTOR_EXTERNAL_ID = "test-connector-ext-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_PROCESSOR_NAME = "TestProcessor";
    private static final JsonNode TEST_DEFINITION = new TextNode("definition");

    @ConfigProperty(name = "managed-connectors.namespace.id")
    String mcNamespaceId;

    @ConfigProperty(name = "managed-connectors.kafka.client.id")
    String serviceAccountId;

    @ConfigProperty(name = "managed-connectors.kafka.client.secret")
    String serviceAccountSecret;

    @Inject
    ConnectorsApiClient connectorsApiClient;
    ConnectorsApi connectorsApi = mock(ConnectorsApi.class);

    @BeforeEach
    public void setup() {
        ((ConnectorsApiClientImpl) connectorsApiClient).setApiSupplier(() -> connectorsApi);
    }

    @Test
    void doGetConnectorGone() throws ApiException {
        final ApiException exception = new ApiException("Gone",
                new IllegalStateException(""),
                Response.Status.GONE.getStatusCode(),
                Collections.emptyMap(), "");

        when(connectorsApi.getConnector(any())).thenThrow(exception);

        assertThat(connectorsApiClient.getConnector(TEST_CONNECTOR_EXTERNAL_ID)).isNull();
    }

    @Test
    void doGetConnectorNotFound() throws ApiException {
        final ApiException exception = new ApiException("Not Found",
                new IllegalStateException(""),
                Response.Status.NOT_FOUND.getStatusCode(),
                Collections.emptyMap(), "");

        when(connectorsApi.getConnector(any())).thenThrow(exception);

        assertThat(connectorsApiClient.getConnector(TEST_CONNECTOR_EXTERNAL_ID)).isNull();
    }

    @Test
    void doGetConnectorApiExceptionUnknown() throws ApiException {
        final ApiException exception = new ApiException("Internal Server Error",
                new IllegalStateException(""),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                Collections.emptyMap(), "");

        when(connectorsApi.getConnector(any())).thenThrow(exception);

        assertThatThrownBy(() -> connectorsApiClient.getConnector(TEST_CONNECTOR_EXTERNAL_ID)).isInstanceOf(ConnectorGetException.class);
    }

    @Test
    void doCreateConnectorApi() throws ApiException {
        when(connectorsApi.createConnector(any(), any())).thenReturn(testConnector());

        Connector connector = connectorsApiClient.createConnector(testConnectorEntity());
        assertThat(connector).isNotNull();
        assertThat(connector.getId()).isEqualTo(TEST_CONNECTOR_EXTERNAL_ID);

        ArgumentCaptor<ConnectorRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ConnectorRequest.class);
        verify(connectorsApi).createConnector(eq(true), requestArgumentCaptor.capture());

        ConnectorRequest connectorRequest = requestArgumentCaptor.getValue();
        assertThat(connectorRequest).isNotNull();

        assertThat(connectorRequest.getName()).isEqualTo(TEST_CONNECTOR_NAME);
        assertThat(connectorRequest.getNamespaceId()).isEqualTo(mcNamespaceId);
        assertThat(connectorRequest.getConnectorTypeId()).isEqualTo(TEST_CONNECTOR_TYPE_ID);
        assertThat(connectorRequest.getServiceAccount().getClientId()).isEqualTo(serviceAccountId);
        assertThat(connectorRequest.getServiceAccount().getClientSecret()).isEqualTo(serviceAccountSecret);
    }

    @Test
    void doCreateConnectorApiWithException() throws ApiException {
        final ApiException exception = new ApiException("Internal Server Error",
                new IllegalStateException(""),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                Collections.emptyMap(), "");

        when(connectorsApi.createConnector(any(), any())).thenThrow(exception);

        assertThatThrownBy(() -> connectorsApiClient.createConnector(testConnectorEntity())).isInstanceOf(ConnectorCreationException.class);
    }

    @Test
    void doDeleteConnector() throws ApiException {
        connectorsApiClient.deleteConnector(TEST_CONNECTOR_ID);

        verify(connectorsApi).deleteConnector(TEST_CONNECTOR_ID);
    }

    @Test
    void doDeleteConnectorApiException() throws ApiException {
        final ApiException exception = new ApiException("Not Found",
                new IllegalStateException(""),
                Response.Status.NOT_FOUND.getStatusCode(),
                Collections.emptyMap(), "");

        when(connectorsApi.deleteConnector(any())).thenThrow(exception);

        assertThatThrownBy(() -> connectorsApiClient.deleteConnector(TEST_CONNECTOR_ID)).isInstanceOf(ConnectorDeletionException.class);
    }

    @Test
    void doDeleteConnectorErrorResponse() throws ApiException {
        final Error error = new Error();

        when(connectorsApi.deleteConnector(any())).thenReturn(error);

        assertThatThrownBy(() -> connectorsApiClient.deleteConnector(TEST_CONNECTOR_ID)).isInstanceOf(ConnectorDeletionException.class);
    }

    @Test
    void doUpdateConnector() throws ApiException {
        connectorsApiClient.updateConnector(TEST_CONNECTOR_ID, TEST_DEFINITION);

        ObjectNode patch = JsonNodeFactory.instance.objectNode();
        patch.set(ConnectorsApiClientImpl.CONNECTOR_PATCH_JSON_PROPERTY, TEST_DEFINITION);

        verify(connectorsApi).patchConnector(TEST_CONNECTOR_ID, patch);
    }

    @Test
    void doUpdateConnectorApiException() throws ApiException {
        final ApiException exception = new ApiException("Not Found",
                new IllegalStateException(""),
                Response.Status.NOT_FOUND.getStatusCode(),
                Collections.emptyMap(), "");

        when(connectorsApi.patchConnector(any(), any())).thenThrow(exception);

        assertThatThrownBy(() -> connectorsApiClient.updateConnector(TEST_CONNECTOR_ID, TEST_DEFINITION)).isInstanceOf(ConnectorUpdateException.class);
    }

    private Connector testConnector() {
        Connector connector = new Connector();
        connector.setId(TEST_CONNECTOR_EXTERNAL_ID);
        return connector;
    }

    private ConnectorEntity testConnectorEntity() {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setType(ConnectorType.SINK);
        connectorEntity.setId(TEST_CONNECTOR_ID);
        connectorEntity.setName(TEST_CONNECTOR_NAME);
        connectorEntity.setConnectorTypeId(TEST_CONNECTOR_TYPE_ID);
        connectorEntity.setConnectorExternalId(TEST_CONNECTOR_EXTERNAL_ID);
        connectorEntity.setProcessor(testProcessor());
        return connectorEntity;
    }

    private Processor testProcessor() {
        Processor processor = new Processor();
        processor.setType(ProcessorType.SINK);
        processor.setId(TEST_PROCESSOR_ID);
        processor.setName(TEST_PROCESSOR_NAME);
        return processor;
    }

}
