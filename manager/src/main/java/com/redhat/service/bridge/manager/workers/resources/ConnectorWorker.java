package com.redhat.service.bridge.manager.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorState;
import com.openshift.cloud.api.connector.models.ConnectorStatusStatus;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.connectors.ConnectorsApiClient;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class ConnectorWorker extends AbstractWorker<ConnectorEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorWorker.class);

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
    public ConnectorEntity createDependencies(Work work, ConnectorEntity connectorEntity) {
        LOGGER.info("Creating dependencies for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());

        // This is idempotent as it gets overridden later depending on actual state
        connectorEntity.setStatus(ManagedResourceStatus.PROVISIONING);
        connectorEntity.setDependencyStatus(ManagedResourceStatus.PROVISIONING);
        connectorEntity = persist(connectorEntity);

        // Step 1 - Create Kafka Topic
        LOGGER.debug("Creating Kafka Topic for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());
        rhoasService.createTopicAndGrantAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);

        // Step 2 - Create Connector
        LOGGER.debug("Creating Managed Connector for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());
        Connector connector = connectorsApi.getConnector(connectorEntity);
        if (Objects.isNull(connector)) {
            LOGGER.debug("Managed Connector for '{}' [{}] not found. Provisioning...",
                    connectorEntity.getName(),
                    connectorEntity.getId());
            // This is an asynchronous operation so exit and wait for it's READY state to be detected on the next poll.
            return deployConnector(connectorEntity);
        }

        // Step 3 - Check it has been provisioned
        ConnectorStatusStatus status = connector.getStatus();
        if (Objects.isNull(status)) {
            LOGGER.debug("Managed Connector status for '{}' [{}] is undetermined.",
                    connectorEntity.getName(),
                    connectorEntity.getId());
            return connectorEntity;
        }
        if (status.getState() == ConnectorState.READY) {
            LOGGER.debug("Managed Connector for '{}' [{}] is ready.",
                    connectorEntity.getName(),
                    connectorEntity.getId());

            // Connector is ready. We can proceed with the deployment of the Processor in the Shard
            // The Processor will be provisioned by the Shard when it is in ACCEPTED state *and* Connectors are READY (or null).
            connectorEntity.setStatus(ManagedResourceStatus.READY);
            connectorEntity.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
            connectorEntity.setDependencyStatus(ManagedResourceStatus.READY);
            return persist(connectorEntity);
        }

        if (status.getState() == ConnectorState.FAILED) {
            LOGGER.debug("Creating Managed Connector for '{}' [{}] failed.",
                    connectorEntity.getName(),
                    connectorEntity.getId());

            // Deployment of the Connector has failed. Bubble FAILED state up to ProcessorWorker.
            connectorEntity.setStatus(ManagedResourceStatus.FAILED);
            return persist(connectorEntity);
        }

        return connectorEntity;
    }

    @Override
    protected boolean isProvisioningComplete(ConnectorEntity managedResource) {
        // As far as the Worker mechanism is concerned work for a Connector is never
        // complete as removal of the Work is controlled by the ProcessorWorker.
        return false;
    }

    private ConnectorEntity deployConnector(ConnectorEntity connectorEntity) {
        // Creation is performed asynchronously. The returned Connector is a place-holder.
        Connector connector = connectorsApi.createConnector(connectorEntity);
        connectorEntity.setConnectorExternalId(connector.getId());
        return persist(connectorEntity);
    }

    @Override
    public ConnectorEntity deleteDependencies(Work work, ConnectorEntity connectorEntity) {
        LOGGER.info("Destroying dependencies for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());

        // This is idempotent as it gets overridden later depending on actual state
        connectorEntity.setStatus(ManagedResourceStatus.DELETING);
        connectorEntity.setDependencyStatus(ManagedResourceStatus.DELETING);
        connectorEntity = persist(connectorEntity);

        Connector connector = connectorsApi.getConnector(connectorEntity);

        // Steps, in reverse order...
        // Step 3 - Connector has been deleted and does not exist: Clean up Kafka Topic
        if (Objects.isNull(connector)) {
            return deleteTopic(connectorEntity);
        }
        ConnectorStatusStatus status = connector.getStatus();
        if (Objects.isNull(status)) {
            return connectorEntity;
        }
        if (status.getState() == ConnectorState.DELETED) {
            LOGGER.debug("Managed Connector for '{}' [{}] has status 'DELETED'. Continuing with deletion of Kafka Topic..",
                    connectorEntity.getName(),
                    connectorEntity.getId());
            return deleteTopic(connectorEntity);
        }
        if (status.getState() == ConnectorState.FAILED) {
            LOGGER.debug("Managed Connector for '{}' [{}] has status 'FAILED'. Continuing with deletion of Kafka Topic..",
                    connectorEntity.getName(),
                    connectorEntity.getId());
            return deleteTopic(connectorEntity);
        }

        // Step 2 - Delete Connector
        LOGGER.debug("Deleting Managed Connector for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());
        connectorsApi.deleteConnector(connectorEntity.getConnectorExternalId());

        return connectorEntity;
    }

    @Override
    protected boolean isDeprovisioningComplete(ConnectorEntity managedResource) {
        // As far as the Worker mechanism is concerned work for a Connector is never
        // complete as removal of the Work is controlled by the ProcessorWorker.
        return false;
    }

    private ConnectorEntity deleteTopic(ConnectorEntity connectorEntity) {
        // Step 1 - Delete Kafka Topic
        LOGGER.debug("Deleting Kafka Topic for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());
        rhoasService.deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);

        return doDeleteDependencies(connectorEntity);
    }

    @Transactional
    protected ConnectorEntity doDeleteDependencies(ConnectorEntity connectorEntity) {
        getDao().deleteById(connectorEntity.getId());
        connectorEntity.setStatus(ManagedResourceStatus.DELETED);
        connectorEntity.setDependencyStatus(ManagedResourceStatus.DELETED);
        return connectorEntity;
    }

}
