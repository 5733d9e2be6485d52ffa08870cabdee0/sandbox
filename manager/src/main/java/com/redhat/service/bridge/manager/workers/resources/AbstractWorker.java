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

        // Fail when we've had enough
        if (areRetriesExceeded(work)) {
            LOGGER.error(
                    "Max retry attempts exceeded trying to create dependencies for '{}' [{}].",
                    managedResource.getName(),
                    managedResource.getId());
            managedResource.setStatus(ManagedResourceStatus.FAILED);
            persist(managedResource);
            return false;
        }
        if (isTimeoutExceeded(work)) {
            LOGGER.error(
                    "Timeout exceeded trying to create dependencies for '{}' [{}].",
                    managedResource.getName(),
                    managedResource.getId());
            managedResource.setStatus(ManagedResourceStatus.FAILED);
            persist(managedResource);
            return false;
        }

        boolean complete = false;
        if (managedResource.getStatus() == ManagedResourceStatus.ACCEPTED) {
            try {
                T updated = createDependencies(managedResource);
                complete = PROVISIONING_COMPLETE.contains(updated.getStatus());
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
                return false;
            }

        } else if (managedResource.getStatus() == ManagedResourceStatus.DEPROVISION) {
            try {
                T updated = deleteDependencies(managedResource);
                complete = DEPROVISIONING_COMPLETE.contains(updated.getStatus());
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
                return false;
            }
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
