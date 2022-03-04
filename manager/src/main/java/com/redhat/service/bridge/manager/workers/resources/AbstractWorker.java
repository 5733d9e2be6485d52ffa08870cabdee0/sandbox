package com.redhat.service.bridge.manager.workers.resources;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.workers.WorkManagerImpl;
import com.redhat.service.bridge.manager.workers.Worker;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public abstract class AbstractWorker<T extends ManagedResource> implements Worker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorker.class);

    private static final Set<ManagedResourceStatus> PROVISIONING_COMPLETE = Set.of(ManagedResourceStatus.READY, ManagedResourceStatus.FAILED);
    private static final Set<ManagedResourceStatus> DEPROVISIONING_COMPLETE = Set.of(ManagedResourceStatus.DELETED, ManagedResourceStatus.FAILED);

    @ConfigProperty(name = "event-bridge.resources.worker.max-retries")
    int maxRetries;

    @ConfigProperty(name = "event-bridge.resources.workers.timeout-seconds")
    int timeoutSeconds;

    @Inject
    WorkManagerImpl workManager;

    @Override
    public boolean handleWork(Work work) {
        //By the time VertX gets to execute an item of Work it may have been deleted.
        if (!workManager.exists(work)) {
            return false;
        }

        T managedResource = load(work);
        if (Objects.isNull(managedResource)) {
            //Work has been scheduled but cannot be found. Something (horribly) wrong has happened.
            String message = String.format("Resource of type '%s' with id '%s' no longer exists in the database.", work.getType(), work.getManagedResourceId());
            throw new IllegalStateException(message);
        }

        boolean complete = false;
        if (managedResource.getStatus() == ManagedResourceStatus.ACCEPTED) {
            complete = PROVISIONING_COMPLETE.contains(createDependencies(work, managedResource).getStatus());
        } else if (managedResource.getStatus() == ManagedResourceStatus.DEPROVISION) {
            complete = DEPROVISIONING_COMPLETE.contains(deleteDependencies(work, managedResource).getStatus());
        }

        if (complete) {
            workManager.complete(work);
        }

        return true;
    }

    @Transactional
    protected T load(Work work) {
        return getDao().findById(work.getManagedResourceId());
    }

    @Transactional
    protected T setStatus(T managedResource, ManagedResourceStatus status) {
        T resource = getDao().findById(managedResource.getId());
        resource.setStatus(status);
        return resource;
    }

    @Transactional
    protected T setDependencyStatus(T managedResource, ManagedResourceStatus status) {
        T resource = getDao().findById(managedResource.getId());
        resource.setDependencyStatus(status);
        return resource;
    }

    protected abstract PanacheRepositoryBase<T, String> getDao();

    @Override
    public T createDependencies(Work work, T managedResource) {
        // Fail when we've had enough
        if (areRetriesExceeded(work)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Max retry attempts exceeded trying to create dependencies for '%s' [%s].",
                        managedResource.getName(),
                        managedResource.getId()));
            }
            return setStatus(managedResource, ManagedResourceStatus.FAILED);
        }
        if (isTimeoutExceeded(work)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Timeout exceeded trying to create dependencies for '%s' [%s].",
                        managedResource.getName(),
                        managedResource.getId()));
            }
            return setStatus(managedResource, ManagedResourceStatus.FAILED);
        }

        try {
            return runCreateOfDependencies(work, managedResource);
        } catch (Exception e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Failed to create dependencies for '%s' [%s].%n"
                        + "Work status: %s%n"
                        + "%s",
                        managedResource.getName(),
                        managedResource.getId(),
                        work,
                        e.getMessage()));
            }
            // Something has gone wrong. We need to retry.
            workManager.recordAttempt(work);
            return setStatus(managedResource, ManagedResourceStatus.FAILED);
        }
    }

    protected boolean areRetriesExceeded(Work w) {
        return w.getAttempts() > maxRetries;
    }

    protected boolean isTimeoutExceeded(Work work) {
        return ZonedDateTime.now().minusSeconds(timeoutSeconds).isAfter(work.getSubmittedAt());
    }

    protected abstract T runCreateOfDependencies(Work work, T managedResource);

    @Override
    public T deleteDependencies(Work work, T managedResource) {
        // Fail when we've had enough
        if (areRetriesExceeded(work)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Max retry attempts exceeded trying to delete dependencies for '%s' [%s].",
                        managedResource.getName(),
                        managedResource.getId()));
            }
            return setStatus(managedResource, ManagedResourceStatus.FAILED);
        }
        if (isTimeoutExceeded(work)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Timeout exceeded trying to delete dependencies for '%s' [%s].",
                        managedResource.getName(),
                        managedResource.getId()));
            }
            return setStatus(managedResource, ManagedResourceStatus.FAILED);
        }

        try {
            return runDeleteOfDependencies(work, managedResource);
        } catch (Exception e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Failed to delete dependencies for '%s' [%s].%n"
                        + "Work status: %s%n"
                        + "%s",
                        managedResource.getName(),
                        managedResource.getId(),
                        work,
                        e.getMessage()));
            }
            // Something has gone wrong. We need to retry.
            workManager.recordAttempt(work);
            return setStatus(managedResource, ManagedResourceStatus.FAILED);
        }
    }

    protected abstract T runDeleteOfDependencies(Work work, T managedResource);

}
