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

    private static final Set<ManagedResourceStatus> PROVISIONING_STARTED = Set.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PROVISIONING);
    private static final Set<ManagedResourceStatus> DEPROVISIONING_STARTED = Set.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING);

    private static final Set<ManagedResourceStatus> PROVISIONING_COMPLETED = Set.of(ManagedResourceStatus.READY, ManagedResourceStatus.FAILED);
    private static final Set<ManagedResourceStatus> DEPROVISIONING_COMPLETED = Set.of(ManagedResourceStatus.DELETED, ManagedResourceStatus.FAILED);

    @ConfigProperty(name = "event-bridge.resources.worker.max-retries")
    int maxRetries;

    @ConfigProperty(name = "event-bridge.resources.workers.timeout-seconds")
    int timeoutSeconds;

    @Inject
    WorkManagerImpl workManager;

    @Override
    public T handleWork(Work work) {
        T managedResource = load(work);
        if (Objects.isNull(managedResource)) {
            //Work has been scheduled but cannot be found. Something (horribly) wrong has happened.
            String message = String.format("Resource of type '%s' with id '%s' no longer exists in the database.", work.getType(), work.getManagedResourceId());
            throw new IllegalStateException(message);
        }

        // Fail when we've had enough
        if (areRetriesExceeded(work)) {
            LOGGER.error(
                    "Max retry attempts exceeded trying to create dependencies for '{}' [{}].",
                    managedResource.getName(),
                    managedResource.getId());
            managedResource.setStatus(ManagedResourceStatus.FAILED);
            persist(managedResource);
            return managedResource;
        }
        if (isTimeoutExceeded(work)) {
            LOGGER.error(
                    "Timeout exceeded trying to create dependencies for '{}' [{}].",
                    managedResource.getName(),
                    managedResource.getId());
            managedResource.setStatus(ManagedResourceStatus.FAILED);
            persist(managedResource);
            return managedResource;
        }

        boolean complete = false;
        T updated = managedResource;
        if (PROVISIONING_STARTED.contains(managedResource.getStatus())) {
            try {
                updated = createDependencies(work, managedResource);
                complete = PROVISIONING_COMPLETED.contains(updated.getStatus());
            } catch (Exception e) {
                LOGGER.info(
                        "Failed to create dependencies for '{}' [{}].\n"
                                + "Work status: {}\n"
                                + "{}",
                        managedResource.getName(),
                        managedResource.getId(),
                        work,
                        e.getMessage());
                // Something has gone wrong. We need to retry.
                workManager.recordAttempt(work);
            }

        } else if (DEPROVISIONING_STARTED.contains(managedResource.getStatus())) {
            try {
                updated = deleteDependencies(work, managedResource);
                complete = DEPROVISIONING_COMPLETED.contains(updated.getStatus());
            } catch (Exception e) {
                LOGGER.info("Failed to delete dependencies for '{}' [{}].\n"
                        + "Work status: {}\n"
                        + "{}",
                        managedResource.getName(),
                        managedResource.getId(),
                        work,
                        e.getMessage());
                // Something has gone wrong. We need to retry.
                workManager.recordAttempt(work);
            }
        }

        if (complete) {
            workManager.complete(work);
        }

        return updated;
    }

    @Transactional
    protected T load(Work work) {
        return getDao().findById(work.getManagedResourceId());
    }

    @Transactional
    protected T persist(T managedResource) {
        return getDao().getEntityManager().merge(managedResource);
    }

    protected abstract PanacheRepositoryBase<T, String> getDao();

    protected boolean areRetriesExceeded(Work w) {
        return w.getAttempts() > maxRetries;
    }

    protected boolean isTimeoutExceeded(Work work) {
        return ZonedDateTime.now().minusSeconds(timeoutSeconds).isAfter(work.getSubmittedAt());
    }

}
