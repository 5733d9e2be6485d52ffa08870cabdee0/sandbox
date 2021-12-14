package com.redhat.service.bridge.manager.connectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.openshift.cloud.api.connector.invoker.ApiClient;
import com.openshift.cloud.api.connector.invoker.ApiException;
import com.openshift.cloud.api.connector.invoker.Configuration;
import com.openshift.cloud.api.connector.invoker.auth.HttpBearerAuth;
import com.openshift.cloud.api.connector.models.Connector;

@RequestScoped
public class ConnectorsApiImpl implements ConnectorsApi {

    @ConfigProperty(name = "managed-connectors.services.url")
    String mcServicesBaseUrl;

    @Inject
    ConnectorsAuth connectorsAuth;

    public Connector createConnector(Connector connector) {
        com.openshift.cloud.api.connector.ConnectorsApi connectorsAPI = createConnectorsAPI();

        try {
            return connectorsAPI.createConnector(true, connector);
        } catch (ApiException e) {
            // TODO-MC error handling
            throw new RuntimeException(e);
        }

    }

    private com.openshift.cloud.api.connector.ConnectorsApi createConnectorsAPI() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(mcServicesBaseUrl);

        HttpBearerAuth Bearer = (HttpBearerAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setBearerToken(connectorsAuth.bearerToken());

        return new com.openshift.cloud.api.connector.ConnectorsApi(defaultClient);
    }
}
