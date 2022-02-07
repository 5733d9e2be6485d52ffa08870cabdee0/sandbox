package com.redhat.service.bridge.manager.connectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

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
            throw new ConnectorCreationException("Error while calling the connectors SKD", e);
        }
    }

    @Override
    public void deleteConnector(String id, String kafkaId) {
        ConnectorsApi connectorsAPI = createConnectorsAPI();

        try {
            Error error = connectorsAPI.deleteConnector(id);
            if (error != null) {
                LOGGER.error("Error while deleting connector with id '{}' and kafkaId '{}', Processor will be scheduled for deletion anyway. Error: '{}'", id, kafkaId, error);
            }
        } catch (ApiException e) {
            LOGGER.error("Error while deleting connector with id '{}' and kafkaId '{}', Processor will be scheduled for deletion anyway", id, kafkaId, e);
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
