package com.redhat.service.bridge.manager.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorRequest;
import com.openshift.cloud.api.connector.models.ConnectorState;
import com.openshift.cloud.api.connector.models.ConnectorStatusStatus;
import com.openshift.cloud.api.connector.models.DeploymentLocation;
import com.openshift.cloud.api.connector.models.KafkaConnectionSettings;
import com.openshift.cloud.api.connector.models.ServiceAccount;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class ConnectorWorker extends AbstractWorker<ConnectorEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorWorker.class);

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
    ConnectorsDAO connectorsDAO;

    @Inject
    RhoasService rhoasService;

    @Inject
    ConnectorsApiClient connectorsApi;

    @Override
    protected PanacheRepositoryBase<ConnectorEntity, String> getDao() {
        return connectorsDAO;
    }

    @Override
    protected ConnectorEntity runCreateOfDependencies(ConnectorEntity connectorEntity) {
        // This is idempotent as it gets overridden later depending on actual state
        connectorEntity = setStatus(connectorEntity, ManagedResourceStatus.PROVISIONING);

        // Step 1 - Create Kafka Topic
        info(LOGGER,
                String.format("Creating Kafka Topic for '%s' [%s]",
                        connectorEntity.getName(),
                        connectorEntity.getId()));
        rhoasService.createTopicAndGrantAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);

        // Step 2 - Create Connector
        info(LOGGER,
                String.format("Creating Managed Connector for '%s' [%s]",
                        connectorEntity.getName(),
                        connectorEntity.getId()));
        Connector connector = connectorsApi.getConnector(connectorEntity);
        if (Objects.isNull(connector)) {
            info(LOGGER,
                    String.format("Managed Connector for '%s' [%s] not found. Provisioning...",
                            connectorEntity.getName(),
                            connectorEntity.getId()));
            // This is an asynchronous operation so exit and wait for it's READY state to be detected on the next poll.
            return deployConnector(connectorEntity);
        }

        // Step 3 - Check it has been provisioned
        ConnectorStatusStatus status = connector.getStatus();
        if (Objects.isNull(status)) {
            return connectorEntity;
        }
        if (status.getState() == ConnectorState.READY) {
            info(LOGGER,
                    String.format("Managed Connector for '%s' [%s] is ready.",
                            connectorEntity.getName(),
                            connectorEntity.getId()));

            // Connector is ready. We can proceed with the deployment of the Processor in the Shard
            // The Processor will be provisioned by the Shard when it is in ACCEPTED state *and* Connectors are READY (or null).
            return setReady(connectorEntity);
        }

        if (status.getState() == ConnectorState.FAILED) {
            info(LOGGER,
                    String.format("Managed Connector for '%s' [%s] failed.",
                            connectorEntity.getName(),
                            connectorEntity.getId()));

            // Deployment of the Connector has failed. Bubble FAILED state up to ProcessorWorker.
            return setStatus(connectorEntity, ManagedResourceStatus.FAILED);
        }

        return connectorEntity;
    }

    private ConnectorEntity deployConnector(ConnectorEntity connectorEntity) {
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

        // Creation is performed asynchronously. The returned Connector is a place-holder.
        Connector c = connectorsApi.createConnector(createConnectorRequest);

        return setConnectorExternalId(connectorEntity, c.getId());
    }

    @Transactional
    protected ConnectorEntity setConnectorExternalId(ConnectorEntity connectorEntity, String connectorExternalId) {
        ConnectorEntity resource = getDao().findById(connectorEntity.getId());
        resource.setConnectorExternalId(connectorExternalId);
        getDao().persist(resource);
        return resource;
    }

    @Override
    public ConnectorEntity deleteDependencies(ConnectorEntity connectorEntity) {
        ConnectorEntity merged = super.deleteDependencies(connectorEntity);

        // Physical deletion of Processors is handled by the Shard calling back to the Manager.
        if (merged.getDependencyStatus().isDeleted()) {
            return doDeleteDependencies(merged);
        }

        return connectorEntity;
    }

    @Override
    protected ConnectorEntity runDeleteOfDependencies(ConnectorEntity connectorEntity) {
        // This is idempotent as it gets overridden later depending on actual state
        connectorEntity = setStatus(connectorEntity, ManagedResourceStatus.DELETING);

        Connector connector = connectorsApi.getConnector(connectorEntity);

        // Step 1 - Clean up Kafka Topic if Connector cannot be found
        if (Objects.isNull(connector)) {
            return deleteTopic(connectorEntity);
        }

        // Step 1 - Clean up Kafka Topic if Connector is DELETED
        ConnectorStatusStatus status = connector.getStatus();
        if (Objects.isNull(status)) {
            return connectorEntity;
        }
        if (status.getState() == ConnectorState.DELETED) {
            return deleteTopic(connectorEntity);
        }
        if (status.getState() == ConnectorState.FAILED) {
            // Deployment of the Connector has failed. Bubble FAILED state up to ProcessorWorker.
            return setStatus(connectorEntity, ManagedResourceStatus.FAILED);
        }

        // Step 2 - Delete Connector
        connectorsApi.deleteConnector(connectorEntity.getConnectorExternalId());

        return connectorEntity;
    }

    private ConnectorEntity deleteTopic(ConnectorEntity connectorEntity) {
        rhoasService.deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);

        return setDeleted(connectorEntity);
    }

    @Transactional
    protected ConnectorEntity setReady(ConnectorEntity connectorEntity) {
        ConnectorEntity resource = getDao().findById(connectorEntity.getId());
        resource.setStatus(ManagedResourceStatus.READY);
        resource.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        resource.getDependencyStatus().setReady(true);
        getDao().persist(resource);
        return resource;
    }

    @Transactional
    protected ConnectorEntity setDeleted(ConnectorEntity connectorEntity) {
        ConnectorEntity resource = getDao().findById(connectorEntity.getId());
        resource.setStatus(ManagedResourceStatus.DELETED);
        resource.getDependencyStatus().setDeleted(true);
        getDao().persist(resource);
        return resource;
    }

    @Transactional
    protected ConnectorEntity doDeleteDependencies(ConnectorEntity connectorEntity) {
        ConnectorEntity merged = getDao().getEntityManager().merge(connectorEntity);
        getDao().delete(merged);
        return merged;
    }

}
