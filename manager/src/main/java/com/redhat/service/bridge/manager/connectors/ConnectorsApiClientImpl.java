package com.redhat.service.bridge.manager.connectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.openshift.cloud.api.connector.ConnectorsApi;
import com.openshift.cloud.api.connector.invoker.ApiClient;
import com.openshift.cloud.api.connector.invoker.ApiException;
import com.openshift.cloud.api.connector.invoker.Configuration;
import com.openshift.cloud.api.connector.invoker.auth.HttpBearerAuth;
import com.openshift.cloud.api.connector.models.Connector;
import com.redhat.service.bridge.manager.exceptions.ConnectorCreationException;

@RequestScoped
public class ConnectorsApiClientImpl implements ConnectorsApiClient {

    @ConfigProperty(name = "managed-connectors.services.url")
    String mcServicesBaseUrl;

    @Inject
    ConnectorsAuth connectorsAuth;

    public Connector createConnector(Connector connector) {
        ConnectorsApi connectorsAPI = createConnectorsAPI();

        try {
            return connectorsAPI.createConnector(true, connector);
        } catch (ApiException e) {
            throw new ConnectorCreationException("Error while calling the connectors SKD", e);
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
