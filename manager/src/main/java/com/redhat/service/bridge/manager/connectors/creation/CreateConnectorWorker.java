package com.redhat.service.bridge.manager.connectors.creation;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.openshift.cloud.api.connector.models.DeploymentLocation;
import com.openshift.cloud.api.connector.models.KafkaConnectionSettings;
import com.openshift.cloud.api.connector.models.ServiceAccount;
import com.redhat.service.bridge.infra.exceptions.BridgeError;
import com.redhat.service.bridge.infra.exceptions.BridgeErrorDAO;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.ConnectorCreationException;
import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;
import com.redhat.service.bridge.manager.connectors.AbstractConnectorWorker;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.connectors.Events;
import com.redhat.service.bridge.manager.models.ConnectorEntity;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class CreateConnectorWorker extends AbstractConnectorWorker<Connector> {

    public static final Duration DEFAULT_BACKOFF = Duration.ofSeconds(1);
    public static final double DEFAULT_JITTER = 0.2;
    public static final int MAX_RETRIES = 10;

    public static final String KAFKA_ID_IGNORED = "kafkaId-ignored";

    @ConfigProperty(name = "managed-connectors.cluster.id")
    String mcClusterId;

    @ConfigProperty(name = "managed-connectors.kafka.bootstrap.servers")
    String kafkaBootstrapServer;

    @ConfigProperty(name = "managed-connectors.kafka.client.id")
    String serviceAccountId;

    @ConfigProperty(name = "managed-connectors.kafka.client.secret")
    String serviceAccountSecret;

    @Inject
    ConnectorsApiClient connectorsApiClient;

    @Inject
    BridgeErrorDAO bridgeErrors;

    @ConsumeEvent(value = Events.KAFKA_TOPIC_CREATED_EVENT, blocking = true)
    public void consume(ConnectorEntity connectorEntity) {
        execute(connectorEntity);
    }

    @Override
    protected Connector callExternalService(ConnectorEntity connectorEntity) {
        JsonNode payload = connectorEntity.getDefinition();
        String newConnectorName = connectorEntity.getName();
        String connectorType = connectorEntity.getConnectorType();

        ConnectorRequest createConnectorRequest = new ConnectorRequest();

        createConnectorRequest.setName(newConnectorName);

        DeploymentLocation deploymentLocation = new DeploymentLocation();
        deploymentLocation.setKind("addon");
        deploymentLocation.setClusterId(mcClusterId);
        createConnectorRequest.setDeploymentLocation(deploymentLocation);

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

        return connectorsApiClient.createConnector(createConnectorRequest);
    }

    @Override
    protected ConnectorEntity updateEntityForSuccess(ConnectorEntity connectorEntity, Connector connector) {
        connectorEntity.setConnectorExternalId(connector.getId());
        connectorEntity.setStatus(ConnectorStatus.MANAGED_CONNECTOR_CREATED);
        return connectorEntity;
    }

    @Override
    protected ConnectorEntity updateEntityForError(ConnectorEntity connectorEntity, Throwable error) {
        connectorEntity.setStatus(ConnectorStatus.FAILED);
        return connectorEntity;
    }

    @Override
    protected void afterSuccessfullyUpdated(ConnectorEntity c) {
        //Check that the Managed Connector has been created
        eventBus.request(Events.MANAGED_CONNECTOR_CREATED_EVENT, c)
                .onFailure()
                .retry()
                .withBackOff(DEFAULT_BACKOFF)
                .withJitter(DEFAULT_JITTER)
                .atMost(MAX_RETRIES)
                .subscribe().with(
                        success -> {
                            /* NOOP */},
                        failure -> {
                            BridgeError error = bridgeErrors.findByException(ConnectorCreationException.class);
                            CreateConnectorWorker.this.errorWhileCalling(new ConnectorCreationException(error.getReason()), c);
                        });
    }

}
