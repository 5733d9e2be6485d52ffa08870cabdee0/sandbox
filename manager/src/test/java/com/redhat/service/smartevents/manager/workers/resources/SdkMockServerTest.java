package com.redhat.service.smartevents.manager.workers.resources;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.openshift.cloud.api.connector.ConnectorsApi;
import com.openshift.cloud.api.connector.invoker.ApiClient;
import com.openshift.cloud.api.connector.invoker.Configuration;
import com.openshift.cloud.api.connector.invoker.auth.HttpBearerAuth;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorStatusStatus;
import com.redhat.service.bridge.test.resource.AppServicesMockResource;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.connectors.ConnectorsApiClient;
import com.redhat.service.smartevents.manager.connectors.ConnectorsApiClientImpl;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.models.Work;
import com.redhat.service.smartevents.manager.workers.WorkManager;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@QuarkusTest
@QuarkusTestResource(value = AppServicesMockResource.class, restrictToAnnotatedClass = true)
public class SdkMockServerTest {

    private static final String TEST_CONNECTOR_EXTERNAL_ID = "c8b3h6ldsp2m5g2vmlug";
    private static final String TEST_RESOURCE_ID = "123";

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    RhoasService rhoasService;

    @Inject
    ConnectorsApiClient connectorsApi;

    @Inject
    WorkManager workManager;

    @ConfigProperty(name = AppServicesMockResource.APP_SERVICE_MOCK_SERVICE)
    String mockUrl;

    private ConnectorWorker worker;

    @BeforeEach
    void setup() {
        ((ConnectorsApiClientImpl) connectorsApi).setApiSupplier(() -> {
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            defaultClient.setBasePath(mockUrl);

            Object object = defaultClient.getAuthentication("Bearer");
            HttpBearerAuth Bearer = (HttpBearerAuth) object;
            Bearer.setBearerToken("BEARER TOKEN");

            return new ConnectorsApi(defaultClient);
        });

        this.worker = new ConnectorWorker();
        this.worker.connectorsDAO = this.connectorsDAO;
        this.worker.rhoasService = this.rhoasService;
        this.worker.connectorsApi = this.connectorsApi;
        this.worker.workManager = this.workManager;
        this.worker.maxRetries = 3;
        this.worker.timeoutSeconds = 60;
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = new Work();
        work.setManagedResourceId(TEST_RESOURCE_ID);

        Connector connector = connectorsApi.getConnector(TEST_CONNECTOR_EXTERNAL_ID);
        ConnectorStatusStatus status = connector.getStatus();
        assertThat(status).isNull();

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }
}
