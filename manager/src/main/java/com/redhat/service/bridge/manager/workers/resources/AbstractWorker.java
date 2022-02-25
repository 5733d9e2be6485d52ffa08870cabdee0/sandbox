package com.redhat.service.bridge.manager.workers.resources;

import javax.inject.Inject;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.models.DependencyStatus;
import com.redhat.service.bridge.manager.models.ManagedEntity;
import com.redhat.service.bridge.manager.workers.Worker;
import com.redhat.service.bridge.manager.workers.Work;
import com.redhat.service.bridge.manager.workers.NaivePostgresDbWorkManagerImpl;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

/*
    Abstract worker implementation. Handles the core logic of:

    - Create dependencies
    - Delete dependencies
    - Respond to work manager requests

    Entity specific logic is delegated out to concrete implementations.

    The core principle here is that Worker inspect their environment to determine if they have made the progress
    required. Once the resources they need to provision are ready, they advance the status of the entity on which
    they are operating.
 */
public abstract class AbstractWorker<T extends ManagedEntity> implements Worker<T> {

    /*
        Should be managed by configuration (and maybe on a per-entity basis)
     */
    private int maxRetries = 3;

    @Inject
    private NaivePostgresDbWorkManagerImpl workManager;

    void handleWork(Work work) {
        T m = getDao().findById(work.getEntityId());
        if (m == null) {
            String message = String.format("Entity of type '%s' with id '%s' no longer exists in the database.", work.getType(), work.getEntityId());
            throw new IllegalStateException(message);
        }
        boolean complete = false;
        if (m.getStatus() == BridgeStatus.ACCEPTED) {
            m = createDependencies(m);
            complete = m.getDependencyStatus().isReady();
        } else if (m.getStatus() == BridgeStatus.DELETED) {
            m = deleteDependencies(m);
            complete = m.getDependencyStatus().isDeleted();
        }

        if (complete) {
            workManager.completeWork(work);
        } else {
            workManager.scheduleWork(m);
        }
    }

    @Override
    public T createDependencies(T managedEntity) {

        if (managedEntity.getStatus() == BridgeStatus.READY || managedEntity.getStatus() == BridgeStatus.FAILED) {
            return managedEntity;
        }

        DependencyStatus dependencyStatus = managedEntity.getDependencyStatus();
        if (areRetriesExceeded(dependencyStatus) || isTimeoutExceeded(managedEntity)) {
            managedEntity.setStatus(BridgeStatus.FAILED);
        } else {
            try {
                runCreateOfDependencies(managedEntity);
            } catch (Exception e) {
                //Something has gone wrong. We need to retry
                managedEntity.getDependencyStatus().recordAttempt();
            }
        }

        return getDao().getEntityManager().merge(managedEntity);
    }

    private boolean isTimeoutExceeded(T managedEntity) {
        //TODO - feels like we need a lastUpdated timestamp on our entities
        return false;
    }

    private boolean areRetriesExceeded(DependencyStatus dependencyStatus) {
        return dependencyStatus.getAttempts() > maxRetries;
    }

    @Override
    public T deleteDependencies(T managedEntity) {
        DependencyStatus dependencyStatus = managedEntity.getDependencyStatus();
        if (areRetriesExceeded(dependencyStatus) || isTimeoutExceeded(managedEntity)) {
            managedEntity.setStatus(BridgeStatus.FAILED);
        } else {
            try {
                runDeleteOfDependencies(managedEntity);
            } catch (Exception e) {
                //Something has gone wrong. We need to retry
                managedEntity.getDependencyStatus().recordAttempt();
            }

            /*
                Everything has been deleted, lets finally delete the entity
             */
            if (managedEntity.getDependencyStatus().isDeleted()) {
                getDao().delete(managedEntity);
            } else {
                managedEntity = getDao().getEntityManager().merge(managedEntity);
            }
        }

        return managedEntity;
    }

    abstract PanacheRepositoryBase<T, String> getDao();

    abstract void runCreateOfDependencies(T managedResource);

    abstract void runDeleteOfDependencies(T managedResource);
}
