package com.redhat.service.smartevents.manager.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorState;
import com.openshift.cloud.api.connector.models.ConnectorStatusStatus;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.ManagedConnectorException;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.connectors.ConnectorsApiClient;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.workers.Work;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;

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
    protected String getId(Work work) {
        // The ID of the ManagedResource to process is the child of that stored directly in the JobDetail.
        String processorId = work.getManagedResourceId();
        ConnectorEntity connectorEntity = connectorsDAO.findByProcessorId(processorId);

        if (Objects.isNull(connectorEntity)) {
            //Work has been scheduled but cannot be found. Something (horribly) wrong has happened.
            throw new IllegalStateException(String.format("Connector for Processor with id '%s' cannot be found in the database.", processorId));
        }

        return connectorEntity.getId();
    }

    @Override
    // This is invoked by Quartz on a context that lacks a RequestContext by default.
    // The Worker calls into ConnectorsApiClient that needs a RequestContext to be active.
    // We therefore activate the RequestContext here.
    @ActivateRequestContext
    public ConnectorEntity createDependencies(Work work, ConnectorEntity connectorEntity) {
        LOGGER.info("Creating dependencies for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());
        // Transition resource to PREPARING status.
        // There is no work handled by the Operator. Connectors move from PREPARING to READY.
        connectorEntity.setStatus(ManagedResourceStatus.PREPARING);

        // This is idempotent as it gets overridden later depending on actual state
        connectorEntity.setDependencyStatus(ManagedResourceStatus.PROVISIONING);
        connectorEntity = persist(connectorEntity);

        // Step 1 - Create Kafka Topic
        LOGGER.debug("Creating Kafka Topic for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());
        rhoasService.createTopicAndGrantAccessFor(connectorEntity.getTopicName(), connectorTopicAccessType(connectorEntity));

        // Step 2 - Create Connector
        LOGGER.debug("Creating Managed Connector for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());
        Optional<Connector> optConnector = Optional.of(connectorEntity)
                .filter(ce -> Objects.nonNull(ce.getConnectorExternalId()) && !ce.getConnectorExternalId().isBlank())
                .map(ConnectorEntity::getConnectorExternalId)
                .map(connectorsApi::getConnector);
        if (optConnector.isEmpty()) {
            LOGGER.debug("Managed Connector for '{}' [{}] not found. Provisioning...",
                    connectorEntity.getName(),
                    connectorEntity.getId());
            // This is an asynchronous operation so exit and wait for it's READY state to be detected on the next poll.
            return deployConnector(connectorEntity);
        }

        // Step 3 - Check it has been provisioned
        Connector connector = optConnector.get();
        ConnectorStatusStatus status = connector.getStatus();
        if (Objects.isNull(status)) {
            LOGGER.debug("Managed Connector status for '{}' [{}] is undetermined.",
                    connectorEntity.getName(),
                    connectorEntity.getId());
            return connectorEntity;
        }
        if (status.getState() == ConnectorState.READY) {
            // If the Connector is ready but differs to that required we need to patch it.
            long processorGeneration = getProcessorGeneration(connectorEntity);
            if (connectorEntity.getGeneration() < processorGeneration) {
                LOGGER.debug("Managed Connector for '{}' [{}] was found but with a different definition. Patching definition.",
                        connectorEntity.getName(),
                        connectorEntity.getId());
                JsonNode updatedConnectorDefinition = connectorEntity.getDefinition();
                connectorsApi.updateConnector(connector.getId(), updatedConnectorDefinition);

                connectorEntity.setGeneration(connectorEntity.getGeneration() + 1);
                return persist(connectorEntity);
            }

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
            LOGGER.debug("Creating Managed Connector for '{}' [{}] failed. Error: {}",
                    connectorEntity.getName(),
                    connectorEntity.getId(),
                    status.getError());

            // Deployment of the Connector has failed. Bubble FAILED state up to ProcessorWorker.
            connectorEntity.setStatus(ManagedResourceStatus.FAILED);
            connectorEntity.setDependencyStatus(ManagedResourceStatus.FAILED);
            persist(connectorEntity);
            return recordError(work, new ManagedConnectorException(status.getError()));
        }

        return connectorEntity;
    }

    @Transactional
    protected long getProcessorGeneration(ConnectorEntity connectorEntity) {
        Processor processor = connectorsDAO.findById(connectorEntity.getId()).getProcessor();
        return processor.getGeneration();
    }

    @Override
    protected boolean isProvisioningComplete(ConnectorEntity managedResource) {
        // As far as the Worker mechanism is concerned work for a Connector is ALWAYS
        // complete as removal of the Work is controlled by the ProcessorWorker.
        return true;
    }

    private ConnectorEntity deployConnector(ConnectorEntity connectorEntity) {
        // Creation is performed asynchronously. The returned Connector is a place-holder.
        Connector connector = connectorsApi.createConnector(connectorEntity);
        connectorEntity.setConnectorExternalId(connector.getId());
        return persist(connectorEntity);
    }

    @Override
    // This is invoked by Quartz on a context that lacks a RequestContext by default.
    // The Worker calls into ConnectorsApiClient that needs a RequestContext to be active.
    // We therefore activate the RequestContext here.
    @ActivateRequestContext
    public ConnectorEntity deleteDependencies(Work work, ConnectorEntity connectorEntity) {
        LOGGER.info("Destroying dependencies for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());

        // This is idempotent as it gets overridden later depending on actual state
        connectorEntity.setStatus(ManagedResourceStatus.DELETING);
        connectorEntity.setDependencyStatus(ManagedResourceStatus.DELETING);
        connectorEntity = persist(connectorEntity);

        // Steps, in reverse order...
        // Step 3 - Connector has been deleted and does not exist: Clean up Kafka Topic
        String connectorExternalId = connectorEntity.getConnectorExternalId();
        if (Objects.isNull(connectorExternalId)) {
            return deleteTopic(connectorEntity);
        }
        Connector connector = connectorsApi.getConnector(connectorExternalId);
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

        // Step 2 - Delete Connector
        LOGGER.debug("Deleting Managed Connector for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());
        connectorsApi.deleteConnector(connectorEntity.getConnectorExternalId());

        return connectorEntity;
    }

    @Override
    protected boolean isDeprovisioningComplete(ConnectorEntity managedResource) {
        // As far as the Worker mechanism is concerned work for a Connector is ALWAYS
        // complete as removal of the Work is controlled by the ProcessorWorker.
        return true;
    }

    private ConnectorEntity deleteTopic(ConnectorEntity connectorEntity) {
        // Step 1 - Delete Kafka Topic
        LOGGER.debug("Deleting Kafka Topic for '{}' [{}]",
                connectorEntity.getName(),
                connectorEntity.getId());
        rhoasService.deleteTopicAndRevokeAccessFor(connectorEntity.getTopicName(), connectorTopicAccessType(connectorEntity));

        return doDeleteDependencies(connectorEntity);
    }

    @Transactional
    protected ConnectorEntity doDeleteDependencies(ConnectorEntity connectorEntity) {
        getDao().deleteById(connectorEntity.getId());
        connectorEntity.setStatus(ManagedResourceStatus.DELETED);
        connectorEntity.setDependencyStatus(ManagedResourceStatus.DELETED);
        return connectorEntity;
    }

    private static RhoasTopicAccessType connectorTopicAccessType(ConnectorEntity connectorEntity) {
        return connectorEntity.getType() == ConnectorType.SOURCE
                ? RhoasTopicAccessType.CONSUMER
                : RhoasTopicAccessType.PRODUCER;
    }

}
