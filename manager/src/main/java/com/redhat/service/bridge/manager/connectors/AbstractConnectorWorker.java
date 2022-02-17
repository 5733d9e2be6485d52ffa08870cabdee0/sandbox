package com.redhat.service.bridge.manager.connectors;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.manager.models.ConnectorEntity;

import io.vertx.mutiny.core.eventbus.EventBus;

// R is the type returned by the external service
public abstract class AbstractConnectorWorker<R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConnectorWorker.class);

    @Inject
    protected EntityManager entityManager;

    @Inject
    protected EventBus eventBus;

    protected abstract R callExternalService(ConnectorEntity connectorEntity);

    // Don't need to implement this if this is a deletion worker
    protected ConnectorEntity updateEntityForSuccess(ConnectorEntity connectorEntity, R serviceResponse) {
        return connectorEntity;
    }

    protected abstract ConnectorEntity updateEntityForError(ConnectorEntity connectorEntity, Throwable error);

    protected void afterSuccessfullyUpdated(ConnectorEntity c) {
    }

    // Used for workers that should delete the entity after successfully calling the service.
    // By default, after the service call updateEntityForSuccess is called, with this physical deletion will occur
    protected boolean shouldDeleteAfterSuccess() {
        return false;
    }

    // Could be the k8s pod name https://issues.redhat.com/browse/MGDOBR-336
    String workerId = UUID.randomUUID().toString();

    // It's important that at the beginning there is no transaction active
    // in this way we're sure each step in the following method is executed in its own transaction
    @Transactional(Transactional.TxType.NEVER)
    public void execute(ConnectorEntity currentEntity) {

        ConnectorEntity connectorEntity;
        try {
            connectorEntity = claimConnector(currentEntity);
            LOGGER.info("Running worker: {} on Entity: {} with workerId: {} ", workerName(), connectorEntity, workerId);
        } catch (Exception e) {
            // Cannot claim, return the unmodified entity
            LOGGER.trace("Worker {} cannot claim Entity: {} with error: {} ", workerId, workerName(), e);
            return;
        }

        try {
            R serviceResponse = callExternalService(connectorEntity);

            if (shouldDeleteAfterSuccess()) { // Used only for workers that physically delete entities
                deleteEntity(connectorEntity);
            } else { // Success path, most common
                ConnectorEntity successConnectorEntity = successfullyCalledService(connectorEntity, serviceResponse);
                afterSuccessfullyUpdated(successConnectorEntity);
            }
        } catch (Exception e) {

            errorWhileCalling(e, connectorEntity);
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public ConnectorEntity claimConnector(ConnectorEntity currentEntity) {
        // As this is a new transaction from a non-transactional state
        // entity is in detached (non managed) state
        // we need to merge it to flush the edit
        ConnectorEntity mergedCE = entityManager.merge(currentEntity);
        mergedCE.setWorkerId(workerId);

        return mergedCE;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public ConnectorEntity successfullyCalledService(ConnectorEntity connectorEntity, R serviceResponse) {
        ConnectorEntity updatedEntity = updateEntityForSuccess(connectorEntity, serviceResponse);
        connectorEntity.setWorkerId(null);
        connectorEntity.setModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

        LOGGER.info("Worker {} successfully called service with response : {} on entity: {} ", workerName(), serviceResponse, updatedEntity);
        // It's important to return the updated entity as successful worker calls will need the entity with the version field updated
        return entityManager.merge(updatedEntity);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void deleteEntity(ConnectorEntity connectorEntity) {
        // Avoid fully load the entity just for deletion
        ConnectorEntity connectorManaged = entityManager.getReference(ConnectorEntity.class, connectorEntity.getId());
        entityManager.remove(connectorManaged);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void errorWhileCalling(Exception error, ConnectorEntity connectorEntity) {
        LOGGER.error("Worker {} failed on Entity: {} with error: {} ", workerName(), connectorEntity, error);

        // By reloading the entity it bypasses the Optimistic Locking
        // We assume that this is called only by one worker as it's after the claim
        ConnectorEntity reloadConnectorEntity = entityManager.find(ConnectorEntity.class, connectorEntity.getId());

        String errorMessage = error.toString();
        reloadConnectorEntity.setError(String.format("%s: %s", workerName(), errorMessage));
        ConnectorEntity updatedConnectorForError = updateEntityForError(reloadConnectorEntity, error);
        updatedConnectorForError.setWorkerId(null);
        updatedConnectorForError.setModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
    }

    private String workerName() {
        return this.getClass().getCanonicalName();
    }
}
