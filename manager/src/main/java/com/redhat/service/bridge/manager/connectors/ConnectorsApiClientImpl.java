package com.redhat.service.bridge.manager.connectors;

import java.util.function.Supplier;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.openshift.cloud.api.connector.ConnectorsApi;
import com.openshift.cloud.api.connector.invoker.ApiClient;
import com.openshift.cloud.api.connector.invoker.ApiException;
import com.openshift.cloud.api.connector.invoker.Configuration;
import com.openshift.cloud.api.connector.invoker.auth.HttpBearerAuth;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.openshift.cloud.api.connector.models.Error;
import com.openshift.cloud.api.connector.models.KafkaConnectionSettings;
import com.openshift.cloud.api.connector.models.ServiceAccount;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.ConnectorCreationException;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.ConnectorDeletionException;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.ConnectorGetException;
import com.redhat.service.bridge.manager.models.ConnectorEntity;

@RequestScoped
public class ConnectorsApiClientImpl implements ConnectorsApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsApiClientImpl.class);

    private static final String KAFKA_ID_IGNORED = "kafkaId-ignored";

    @ConfigProperty(name = "managed-connectors.services.url")
    String mcServicesBaseUrl;

    @ConfigProperty(name = "managed-connectors.namespace.id")
    String mcNamespaceId;

    @ConfigProperty(name = "managed-connectors.kafka.bootstrap.servers")
    String kafkaBootstrapServer;

    @ConfigProperty(name = "managed-connectors.kafka.client.id")
    String serviceAccountId;

    @ConfigProperty(name = "managed-connectors.kafka.client.secret")
    String serviceAccountSecret;

    @Inject
    ConnectorsOidcClient connectorsAuth;

    // The API is provided by a Supplier to (easily) support overriding it for Unit Tests
    private Supplier<ConnectorsApi> apiSupplier = () -> {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(mcServicesBaseUrl);

        HttpBearerAuth Bearer = (HttpBearerAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setBearerToken(connectorsAuth.getToken());

        return new com.openshift.cloud.api.connector.ConnectorsApi(defaultClient);
    };

    @Override
    public Connector getConnector(String connectorExternalId) {
        ConnectorsApi connectorsAPI = createConnectorsAPI();

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Retrieving Connector with ID '%s'", connectorExternalId));
            }
            return connectorsAPI.getConnector(connectorExternalId);
        } catch (ApiException e) {
            if (e.getCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }
            throw new ConnectorGetException("Error while retrieving the connector on MC Fleet Manager", e);
        }
    }

    @Override
    public Connector createConnector(ConnectorRequest connectorRequest) {
        ConnectorsApi connectorsAPI = createConnectorsAPI();

        try {
            return connectorsAPI.createConnector(true, connectorRequest);
        } catch (ApiException e) {
            throw new ConnectorCreationException("Error while creating the connector on MC Fleet Manager", e);
        }
    }

    @Override
    public Connector createConnector(ConnectorEntity connectorEntity) {
        ConnectorRequest createConnectorRequest = new ConnectorRequest();

        String newConnectorName = connectorEntity.getName();
        createConnectorRequest.setName(newConnectorName);

        createConnectorRequest.setNamespaceId(mcNamespaceId);

        String connectorType = connectorEntity.getConnectorType();
        JsonNode payload = connectorEntity.getDefinition();
        createConnectorRequest.setConnectorTypeId(connectorType);
        createConnectorRequest.setConnector(payload);

        ServiceAccount serviceAccount = new ServiceAccount();
        serviceAccount.setClientId(serviceAccountId);
        serviceAccount.setClientSecret(serviceAccountSecret);
        createConnectorRequest.setServiceAccount(serviceAccount);

        KafkaConnectionSettings kafka = new KafkaConnectionSettings();
        kafka.setUrl(kafkaBootstrapServer);
        // https://issues.redhat.com/browse/MGDOBR-198
        // this is currently ignored in the Connectors API
        kafka.setId(KAFKA_ID_IGNORED);
        createConnectorRequest.setKafka(kafka);

        return createConnector(createConnectorRequest);
    }

    @Override
    public void deleteConnector(String id) {
        ConnectorsApi connectorsAPI = createConnectorsAPI();

        try {
            Error error = connectorsAPI.deleteConnector(id);
            if (error != null) {
                throw new ConnectorDeletionException("Error while deleting the connector on MC Fleet Manager: " + error);
            }
        } catch (ApiException e) {
            throw new ConnectorDeletionException("Error while deleting the connector on MC Fleet Manager", e);
        }
    }

    void setApiSupplier(Supplier<ConnectorsApi> apiSupplier) {
        this.apiSupplier = apiSupplier;
    }

    private ConnectorsApi createConnectorsAPI() {
        return apiSupplier.get();
    }
}
