package com.redhat.service.smartevents.manager.v2.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ProvisioningMaxRetriesExceededException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ProvisioningTimeOutException;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.core.models.ManagedResource;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.core.workers.Worker;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ConditionDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ManagedResourceV2DAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import static com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ProvisioningMaxRetriesExceededException.RETRIES_FAILURE_MESSAGE;
import static com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.ProvisioningTimeOutException.TIMEOUT_FAILURE_MESSAGE;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.DELETED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.DELETING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.DEPROVISION;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.PREPARING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;

public abstract class AbstractWorker<T extends ManagedResourceV2> implements Worker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorker.class);

    private static final Set<ManagedResourceStatus> PROVISIONING_STARTED = Set.of(ACCEPTED, PREPARING);
    private static final Set<ManagedResourceStatus> DEPROVISIONING_STARTED = Set.of(DEPROVISION, DELETING);

    protected static final Set<ManagedResourceStatus> PROVISIONING_COMPLETED = Set.of(READY, FAILED);
    protected static final Set<ManagedResourceStatus> DEPROVISIONING_COMPLETED = Set.of(DELETED, FAILED);

    @ConfigProperty(name = "event-bridge.resources.worker.max-retries")
    int maxRetries;

    @ConfigProperty(name = "event-bridge.resources.workers.timeout-seconds")
    int timeoutSeconds;

    @V2
    @Inject
    WorkManager workManager;

    @Inject
    ConditionDAO conditionDAO;

    @Inject
    BridgeErrorHelper bridgeErrorHelper;

    @Override
    public T handleWork(Work work) {
        String id = getId(work);
        T managedResource = load(id);
        if (Objects.isNull(managedResource)) {
            //Work has been scheduled but cannot be found. Something (horribly) wrong has happened.
            throw new IllegalStateException(String.format("Resource of type '%s' with id '%s' no longer exists in the database.", work.getType(), id));
        }

        // Fail when we've had enough
        boolean areRetriesExceeded = areRetriesExceeded(work, managedResource);
        boolean isTimeoutExceeded = isTimeoutExceeded(work, managedResource);
        if (areRetriesExceeded || isTimeoutExceeded) {
            InternalPlatformException failure;
            if (areRetriesExceeded) {
                failure = new ProvisioningMaxRetriesExceededException(String.format(RETRIES_FAILURE_MESSAGE,
                        work.getType(),
                        work.getManagedResourceId()));
            } else {
                failure = new ProvisioningTimeOutException(String.format(TIMEOUT_FAILURE_MESSAGE,
                        work.getType(),
                        work.getManagedResourceId()));
            }

            return recordError(work, failure);
        }

        T updated = managedResource;
        if (OperationType.CREATE.equals(managedResource.getOperation().getType()) || OperationType.UPDATE.equals(managedResource.getOperation().getType())) {
            try {
                updated = createDependencies(work, managedResource);
            } catch (Exception e) {
                LOGGER.error(String.format("Failed to create dependencies for resource of type '%s' with id '%s'.", work.getType(), id), e);
                // Something has gone wrong. We need to retry.
                workManager.rescheduleAfterFailure(work);
            } finally {
                if (!isProvisioningComplete(updated)) {
                    workManager.reschedule(work);
                }
            }
        } else if (OperationType.DELETE.equals(managedResource.getOperation().getType())) {
            try {
                updated = deleteDependencies(work, managedResource);
            } catch (Exception e) {
                LOGGER.info(String.format("Failed to delete dependencies for resource of type '%s' with id '%s'.", work.getType(), id), e);
                // Something has gone wrong. We need to retry.
                workManager.rescheduleAfterFailure(work);
            } finally {
                if (!isDeprovisioningComplete(updated)) {
                    workManager.reschedule(work);
                }
            }
        }

        return updated;
    }

    public abstract String getId(Work work);

    @Transactional
    protected T load(String id) {
        return getDao().findByIdWithConditions(id);
    }

    @Transactional
    protected T persist(T managedResource) {
        return getDao().getEntityManager().merge(managedResource);
    }

    protected boolean areRetriesExceeded(Work w, ManagedResource managedResource) {
        boolean areRetriesExceeded = w.getAttempts() > maxRetries;
        if (areRetriesExceeded) {
            LOGGER.error(
                    "Max retry attempts exceeded trying to create dependencies for '{}' [{}].",
                    managedResource.getName(),
                    managedResource.getId());
        }
        return areRetriesExceeded;
    }

    protected boolean isTimeoutExceeded(Work work, ManagedResource managedResource) {
        boolean isTimeoutExceeded = ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(timeoutSeconds).isAfter(work.getSubmittedAt());
        if (isTimeoutExceeded) {
            LOGGER.error(
                    "Timeout exceeded trying to create dependencies for '{}' [{}].",
                    managedResource.getName(),
                    managedResource.getId());
        }
        return isTimeoutExceeded;
    }

    protected T recordError(Work work, Exception e) {
        String managedResourceId = getId(work);
        T managedResource = load(managedResourceId);
        BridgeErrorInstance bridgeErrorInstance = bridgeErrorHelper.getBridgeErrorInstance(e);

        // Add failed condition with details.

        return persist(managedResource);
    }

    public abstract ManagedResourceV2DAO<T> getDao();

    protected abstract T createDependencies(Work work, T managedResource);

    @Transactional
    protected <R> R executeWithFailureRecording(String conditionType, T managedResource, Callable<R> function) {
        Condition condition = findConditionByType(conditionType, managedResource);
        condition = conditionDAO.getEntityManager().getReference(Condition.class, condition.getId());
        try {
            R result = function.call();

            condition.setType(conditionType);
            condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));
            condition.setStatus(ConditionStatus.TRUE);

            return result;
        } catch (Exception e) {
            BridgeErrorInstance bridgeErrorInstance = bridgeErrorHelper.getBridgeErrorInstance(e);
            condition.setErrorCode(bridgeErrorInstance.getCode());
            condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));
            condition.setStatus(ConditionStatus.FALSE);
            condition.setMessage("Failed to deploy " + conditionType + " due to " + e.getMessage());
            condition.setReason(bridgeErrorInstance.getReason());

            throw new RuntimeException(e);
        }
    }

    protected abstract T deleteDependencies(Work work, T managedResource);

    // When Work is "complete" the Work is removed from the Work Queue acted on by WorkManager.
    // For simple two-step chains (e.g. our existing Processor->Connector resource) it does not matter
    // a great deal if either Processor or Connector decide the work is complete. However, consider a three-step
    // chain: A->B->C where A is the primary work, B a dependency on A and C a dependency on B. We would not want
    // the work for A to be removed from the Work Queue until B and C are complete. Therefore, neither B nor C should
    // flag that work is complete. B would check on the status of C and A on B. These methods allow for this.
    public boolean isProvisioningComplete(T managedResource) {
        // The resource is in PROVISIONING if all the MANAGER conditions are READY.
        return StatusUtilities.managerDependenciesCompleted(managedResource);
    }

    public boolean isDeprovisioningComplete(T managedResource) {
        // TODO: we don't have a state to figure out that we have deleted all the dependencies.
        return StatusUtilities.managerDependenciesCompleted(managedResource);
    }

    private Condition findConditionByType(String conditionType, T managedResource) {
        Optional<Condition> condition = managedResource.getConditions().stream().filter(x -> conditionType.equals(x.getType())).findFirst();

        // The condition should be already there. But in case it is not there, we log and create it.
        return condition.orElseThrow(() -> {
            String message = String.format("Condition '%s' not found for resource '%s'.", conditionType, managedResource);
            LOGGER.error(message);
            return new IllegalStateException(message);
        });
    }
}
