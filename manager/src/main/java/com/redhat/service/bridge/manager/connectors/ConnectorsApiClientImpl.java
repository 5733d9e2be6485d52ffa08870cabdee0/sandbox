package com.redhat.service.bridge.manager.connectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.redhat.service.bridge.manager.models.ConnectorEntity;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.cloud.api.connector.ConnectorsApi;
import com.openshift.cloud.api.connector.invoker.ApiClient;
import com.openshift.cloud.api.connector.invoker.ApiException;
import com.openshift.cloud.api.connector.invoker.Configuration;
import com.openshift.cloud.api.connector.invoker.auth.HttpBearerAuth;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.openshift.cloud.api.connector.models.Error;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.ConnectorCreationException;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.ConnectorDeletionException;

@RequestScoped
public class ConnectorsApiClientImpl implements ConnectorsApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorsApiClientImpl.class);

    @ConfigProperty(name = "managed-connectors.services.url")
    String mcServicesBaseUrl;

    @Inject
    ConnectorsAuth connectorsAuth;

    @Override
    public Connector createConnector(ConnectorRequest connector) {
        ConnectorsApi connectorsAPI = createConnectorsAPI();

        try {
            return connectorsAPI.createConnector(true, connector);
        } catch (ApiException e) {
            throw new ConnectorCreationException("Error while creating the connector on MC Fleet Manager", e);
        }
    }

    @Override
    public Connector getConnector(ConnectorEntity connectorEntity) {
        ConnectorsApi connectorsAPI = createConnectorsAPI();

        try {
            String connectorExternalId = connectorEntity.getConnectorExternalId();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Retrieving Connector with ID '%s'", connectorExternalId));
            }
            return connectorsAPI.getConnector(connectorExternalId);
        } catch (ApiException e) {
            if (e.getCode() != Response.Status.NOT_FOUND.getStatusCode()) {
                throw new ConnectorCreationException("Error while retrieving the connector on MC Fleet Manager", e);
            }
        }
        return null;
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

    private ConnectorsApi createConnectorsAPI() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(mcServicesBaseUrl);

        HttpBearerAuth Bearer = (HttpBearerAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setBearerToken(connectorsAuth.bearerToken());

        return new com.openshift.cloud.api.connector.ConnectorsApi(defaultClient);
    }
}
