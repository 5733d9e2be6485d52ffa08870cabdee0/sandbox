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
    protected ConnectorEntity runCreateOfDependencies(Work work, ConnectorEntity connectorEntity) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Creating dependencies for '%s' [%s]",
                    connectorEntity.getName(),
                    connectorEntity.getId()));
        }

        // This is idempotent as it gets overridden later depending on actual state
        connectorEntity = setStatus(connectorEntity, ManagedResourceStatus.PROVISIONING);

        // Step 1 - Create Kafka Topic
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Creating Kafka Topic for '%s' [%s]",
                    connectorEntity.getName(),
                    connectorEntity.getId()));
        }
        rhoasService.createTopicAndGrantAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);

        // Step 2 - Create Connector
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Creating Managed Connector for '%s' [%s]",
                    connectorEntity.getName(),
                    connectorEntity.getId()));
        }
        Connector connector = connectorsApi.getConnector(connectorEntity);
        if (Objects.isNull(connector)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Managed Connector for '%s' [%s] not found. Provisioning...",
                        connectorEntity.getName(),
                        connectorEntity.getId()));
            }
            // This is an asynchronous operation so exit and wait for it's READY state to be detected on the next poll.
            return deployConnector(connectorEntity);
        }

        // Step 3 - Check it has been provisioned
        ConnectorStatusStatus status = connector.getStatus();
        if (Objects.isNull(status)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Managed Connector status for '%s' [%s] is undetermined.",
                        connectorEntity.getName(),
                        connectorEntity.getId()));
            }
            return connectorEntity;
        }
        if (status.getState() == ConnectorState.READY) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Managed Connector for '%s' [%s] is ready.",
                        connectorEntity.getName(),
                        connectorEntity.getId()));
            }

            // Connector is ready. We can proceed with the deployment of the Processor in the Shard
            // The Processor will be provisioned by the Shard when it is in ACCEPTED state *and* Connectors are READY (or null).
            return setReady(connectorEntity);
        }

        if (status.getState() == ConnectorState.FAILED) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Creating Managed Connector for '%s' [%s] failed.",
                        connectorEntity.getName(),
                        connectorEntity.getId()));
            }

            // Deployment of the Connector has failed. Bubble FAILED state up to ProcessorWorker.
            return setStatus(connectorEntity, ManagedResourceStatus.FAILED);
        }

        return connectorEntity;
    }

    private ConnectorEntity deployConnector(ConnectorEntity connectorEntity) {
        // Creation is performed asynchronously. The returned Connector is a place-holder.
        Connector connector = connectorsApi.createConnector(connectorEntity);
        return setConnectorExternalId(connectorEntity, connector.getId());
    }

    @Transactional
    protected ConnectorEntity setConnectorExternalId(ConnectorEntity connectorEntity, String connectorExternalId) {
        ConnectorEntity resource = getDao().findById(connectorEntity.getId());
        resource.setConnectorExternalId(connectorExternalId);
        return resource;
    }

    @Override
    protected ConnectorEntity runDeleteOfDependencies(Work work, ConnectorEntity connectorEntity) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Destroying dependencies for '%s' [%s]",
                    connectorEntity.getName(),
                    connectorEntity.getId()));
        }

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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Deleting Managed Connector for '%s' [%s] failed.",
                        connectorEntity.getName(),
                        connectorEntity.getId()));
            }

            // Deployment of the Connector has failed. Bubble FAILED state up to ProcessorWorker.
            return setStatus(connectorEntity, ManagedResourceStatus.FAILED);
        }

        // Step 2 - Delete Connector
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Deleting Managed Connector for '%s' [%s]",
                    connectorEntity.getName(),
                    connectorEntity.getId()));
        }
        connectorsApi.deleteConnector(connectorEntity.getConnectorExternalId());

        return connectorEntity;
    }

    private ConnectorEntity deleteTopic(ConnectorEntity connectorEntity) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Deleting Kafka Topic for '%s' [%s]",
                    connectorEntity.getName(),
                    connectorEntity.getId()));
        }
        rhoasService.deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), RhoasTopicAccessType.PRODUCER);

        return doDeleteDependencies(connectorEntity);
    }

    @Transactional
    protected ConnectorEntity setReady(ConnectorEntity connectorEntity) {
        ConnectorEntity resource = getDao().findById(connectorEntity.getId());
        resource.setStatus(ManagedResourceStatus.READY);
        resource.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        resource.setDependencyStatus(ManagedResourceStatus.READY);
        return resource;
    }

    @Transactional
    protected ConnectorEntity doDeleteDependencies(ConnectorEntity connectorEntity) {
        getDao().deleteById(connectorEntity.getId());
        connectorEntity.setStatus(ManagedResourceStatus.DELETED);
        return connectorEntity;
    }

}
