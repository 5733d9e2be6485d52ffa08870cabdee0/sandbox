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
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.workers.WorkManager;
import com.redhat.service.bridge.manager.workers.Worker;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public abstract class AbstractWorker<T extends ManagedResource> implements Worker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorker.class);

    @ConfigProperty(name = "event-bridge.resources.worker.max-retries")
    int maxRetries;

    @ConfigProperty(name = "event-bridge.resources.workers.timeout-seconds")
    int timeoutSeconds;

    @Inject
    WorkManager workManager;

    @Override
    public boolean handleWork(Work work) {
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
            managedResource.setDependencyStatus(ManagedResourceStatus.FAILED);
            persist(managedResource);
            return true;
        }
        if (isTimeoutExceeded(work)) {
            LOGGER.error(
                    "Timeout exceeded trying to create dependencies for '{}' [{}].",
                    managedResource.getName(),
                    managedResource.getId());
            managedResource.setStatus(ManagedResourceStatus.FAILED);
            managedResource.setDependencyStatus(ManagedResourceStatus.FAILED);
            persist(managedResource);
            return true;
        }

        boolean complete = false;
        T updated;
        if (PROVISIONING_STARTED.contains(managedResource.getStatus())) {
            try {
                updated = createDependencies(work, managedResource);
                complete = isProvisioningComplete(updated);
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
                complete = isDeprovisioningComplete(updated);
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

        return complete;
    }

    @Transactional
    protected T load(Work work) {
        return getDao().findById(work.getManagedResourceId());
    }

    @Transactional
    protected T persist(T managedResource) {
        managedResource.setModifiedAt(ZonedDateTime.now());
        return getDao().getEntityManager().merge(managedResource);
    }

    protected boolean areRetriesExceeded(Work w) {
        return w.getAttempts() > maxRetries;
    }

    protected boolean isTimeoutExceeded(Work work) {
        return ZonedDateTime.now().minusSeconds(timeoutSeconds).isAfter(work.getSubmittedAt());
    }

    protected abstract PanacheRepositoryBase<T, String> getDao();

    protected abstract T createDependencies(Work work, T managedResource);

    protected abstract T deleteDependencies(Work work, T managedResource);

    protected boolean isProvisioningComplete(T managedResource) {
        return PROVISIONING_COMPLETED.contains(managedResource.getStatus());
    }

    protected boolean isDeprovisioningComplete(T managedResource) {
        return DEPROVISIONING_COMPLETED.contains(managedResource.getStatus());
    }

}
