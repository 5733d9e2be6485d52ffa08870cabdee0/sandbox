package com.redhat.service.bridge.manager.workers.resources;

import java.time.ZonedDateTime;
import java.util.Objects;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.workers.Work;
import com.redhat.service.bridge.manager.workers.WorkManagerImpl;
import com.redhat.service.bridge.manager.workers.Worker;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public abstract class AbstractWorker<T extends ManagedResource> implements Worker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorker.class);

    @ConfigProperty(name = "event-bridge.resources.worker.max-retries")
    int maxRetries;

    @ConfigProperty(name = "event-bridge.resources.workers.timeout-seconds")
    int timeoutSeconds;

    @Inject
    WorkManagerImpl workManager;

    @Override
    public void handleWork(Work work) {
        //By the time VertX gets to execute an item of Work it may have been deleted.
        if (!workManager.exists(work)) {
            return;
        }

        T managedResource = load(work);
        if (Objects.isNull(managedResource)) {
            //Work has been scheduled but cannot be found. Something (horribly) wrong has happened.
            String message = String.format("Resource of type '%s' with id '%s' no longer exists in the database.", work.getType(), work.getManagedResourceId());
            throw new IllegalStateException(message);
        }

        boolean complete = false;
        if (managedResource.getStatus() == ManagedResourceStatus.ACCEPTED) {
            complete = createDependencies(managedResource).getDependencyStatus().isReady();
        } else if (managedResource.getStatus() == ManagedResourceStatus.DEPROVISION) {
            complete = deleteDependencies(managedResource).getDependencyStatus().isDeleted();
        }

        if (complete) {
            workManager.complete(work);
        } else {
            workManager.schedule(managedResource);
        }
    }

    @Transactional
    protected T load(Work work) {
        return getDao().findById(work.getManagedResourceId());
    }

    @Transactional
    protected T setStatus(T managedResource, ManagedResourceStatus status) {
        T resource = getDao().findById(managedResource.getId());
        resource.setStatus(status);
        getDao().persist(resource);
        return resource;
    }

    @Transactional
    protected T recordAttempt(T managedResource) {
        T resource = getDao().findById(managedResource.getId());
        resource.getDependencyStatus().recordAttempt();
        getDao().persist(resource);
        return resource;
    }

    @Transactional
    protected T setDependencyReady(T managedResource, boolean ready) {
        T resource = getDao().findById(managedResource.getId());
        resource.getDependencyStatus().setReady(ready);
        getDao().persist(resource);
        return resource;
    }

    @Transactional
    protected T setDependencyDeleted(T managedResource, boolean deleted) {
        T resource = getDao().findById(managedResource.getId());
        resource.getDependencyStatus().setDeleted(deleted);
        getDao().persist(resource);
        return resource;
    }

    protected abstract PanacheRepositoryBase<T, String> getDao();

    @Override
    public T createDependencies(T managedResource) {
        // Don't process resources that have their Work completed
        if (managedResource.getStatus() == ManagedResourceStatus.READY || managedResource.getStatus() == ManagedResourceStatus.FAILED) {
            return managedResource;
        }

        // Fail when we've had enough
        if (areRetriesExceeded(managedResource)) {
            info(LOGGER,
                    String.format("Max retry attempts exceeded trying to create dependencies for '%s' [%s].",
                            managedResource.getName(),
                            managedResource.getId()));
            return setStatus(managedResource, ManagedResourceStatus.FAILED);
        }
        if (isTimeoutExceeded(managedResource)) {
            info(LOGGER,
                    String.format("Timeout exceeded trying to create dependencies for '%s' [%s].",
                            managedResource.getName(),
                            managedResource.getId()));
            return setStatus(managedResource, ManagedResourceStatus.FAILED);
        }

        try {
            return runCreateOfDependencies(managedResource);
        } catch (Exception e) {
            info(LOGGER,
                    String.format("Failed to create dependencies for '%s' [%s].%nDependency status: %s%n%s",
                            managedResource.getName(),
                            managedResource.getId(),
                            managedResource.getDependencyStatus(),
                            e.getMessage()));
            // Something has gone wrong. We need to retry.
            return recordAttempt(managedResource);
        }
    }

    @Transactional
    protected boolean areRetriesExceeded(T managedEntity) {
        //This needs to be in a transaction as the child is loaded lazily
        return getDao().findById(managedEntity.getId()).getDependencyStatus().getAttempts() > maxRetries;
    }

    @Transactional
    protected boolean isTimeoutExceeded(T managedEntity) {
        //This needs to be in a transaction as the child is loaded lazily
        return ZonedDateTime.now().minusSeconds(timeoutSeconds).isAfter(getDao().findById(managedEntity.getId()).getSubmittedAt());
    }

    protected abstract T runCreateOfDependencies(T managedResource);

    @Override
    public T deleteDependencies(T managedResource) {
        // Fail when we've had enough
        if (areRetriesExceeded(managedResource) || isTimeoutExceeded(managedResource)) {
            return setStatus(managedResource, ManagedResourceStatus.FAILED);
        }

        try {
            return runDeleteOfDependencies(managedResource);
        } catch (Exception e) {
            info(LOGGER,
                    String.format("Failed to delete dependencies for '%s' [%s].%nDependency status: %s%n%s",
                            managedResource.getName(),
                            managedResource.getId(),
                            managedResource.getDependencyStatus(),
                            e.getMessage()));
            // Something has gone wrong. We need to retry.
            return recordAttempt(managedResource);
        }
    }

    protected abstract T runDeleteOfDependencies(T managedResource);

    protected void info(Logger logger, String message) {
        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }

}
