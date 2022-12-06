package com.redhat.service.smartevents.manager.v2.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
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

public abstract class AbstractWorker<T extends ManagedResourceV2> implements Worker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorker.class);

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
            // Transition all the Conditions with status != READY to FAILED.
            return recordError(work);
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

    protected T recordError(Work work) {
        String managedResourceId = getId(work);
        T managedResource = load(managedResourceId);

        markFailedConditions(managedResource);

        return persist(managedResource);
    }

    public abstract ManagedResourceV2DAO<T> getDao();

    protected abstract T createDependencies(Work work, T managedResource);

    /**
     * Deploys/Removes a dependency from a worker.
     * The <code>onResult</code> callback is meant to contain the logic to transition the <code>Condition</code> accordingly.
     * In some cases it is necessary to parse the result of the <code>function</code> to understand if the dependency was properly deployed: the <code>onResult</code>
     * callback is providing such flexibility.
     * The <code>onException</code> callback is meant to handle exceptions raised by the <code>function</code>.
     *
     * Default implementations for the <code>onResult</code> and the <code>onException</code> are provided by the <code>defaultOnResult</code> and <code>defaultOnException</code> methods
     * of this class.
     *
     * Note that @Transactional is meant to roll back when something goes wrong (an exception is thrown). Given that we need to
     * catch, change the condition status and rethrow, we have have to dontRollbackOn=Exception.class.
     *
     * @param conditionType: The condition name of the dependency
     * @param managedResource The managed resource
     * @param function The function to deploy/undeploy a dependency
     * @param onResult The callback with the result of the <code>function</code>. It should parse the result of the <code>function</code> and set the condition status accordingly.
     * @param onException The callback to handle the exception raised by the <code>function</code>.
     * @param <R> Generic for the returned object of the <code>function</code>.
     * @return The returned object of the <code>function</code>.
     */
    @Transactional(dontRollbackOn = { Exception.class })
    protected <R> R executeWithFailureRecording(String conditionType, T managedResource, Callable<R> function, BiFunction<R, Condition, Condition> onResult,
            BiFunction<Exception, Condition, Condition> onException) {
        Condition condition = findConditionByType(conditionType, managedResource);
        Condition conditionRef = conditionDAO.getEntityManager().getReference(Condition.class, condition.getId());
        try {
            R result = function.call();
            Condition modified = onResult.apply(result, condition);
            Condition.copy(modified, conditionRef);
            return result;
        } catch (Exception e) {
            Condition modified = onException.apply(e, condition);
            Condition.copy(modified, conditionRef);
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates the default <code>BiFunction</code> to be used in <code>executeWithFailureRecording</code>.
     * In particular, if the <code>function</code> of <code>executeWithFailureRecording</code> is supposed to deploy
     * the dependency immediately, the condition can be updated immediately.
     * Don't use this default implementation in case you have to parse the result of the <code>function</code> to decide
     * if the dependency was properly deployed or not. For example, some dependencies might take minutes to be deployed.
     *
     * With this default implementation, the condition is set to TRUE regardless the result of the <code>function</code>.
     *
     * @param type Condition type
     * @param <R> Generic for the returned object of the <code>function</code>.
     * @return The modified condition.
     */
    protected <R> BiFunction<R, Condition, Condition> defaultOnResult(String type) {
        return (o, condition) -> {
            condition.setType(type);
            condition.setStatus(ConditionStatus.TRUE);
            condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));
            return condition;
        };
    }

    /**
     * Generates the default <code>BiFunction</code> to be used in <code>executeWithFailureRecording</code>.
     * Don't use this default implementation in case you have to handle exceptions in a custom way: with this default implementation,
     * in case of an exception the condition is marked as FALSE.
     *
     * @param type Condition type
     * @param <R> Generic for the returned object of the <code>function</code>.
     * @return The modified condition.
     */
    protected BiFunction<Exception, Condition, Condition> defaultOnException(String type) {
        return (e, condition) -> {
            BridgeErrorInstance bridgeErrorInstance = bridgeErrorHelper.getBridgeErrorInstance(e);
            condition.setErrorCode(bridgeErrorInstance.getCode());
            condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));
            condition.setStatus(ConditionStatus.FALSE);
            condition.setMessage("Failed to deploy " + type + " due to " + e.getMessage());
            condition.setReason(bridgeErrorInstance.getReason());
            return condition;
        };
    }

    @Transactional
    protected void markFailedConditions(T managedResource) {
        // Mark all the Manager conditions with status != TRUE as failed.
        for (Condition condition : managedResource.getConditions()) {
            if (ComponentType.MANAGER.equals(condition.getComponent()) && !ConditionStatus.TRUE.equals(condition.getStatus())) {
                condition = conditionDAO.getEntityManager().getReference(Condition.class, condition.getId());
                condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));
                condition.setStatus(ConditionStatus.FAILED);
                // Don't overwrite the message and the reason to keep track of the original failures (if it was recorded).
            }
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
        return StatusUtilities.managerDependenciesCompleted(managedResource);
    }

    public boolean isDeprovisioningComplete(T managedResource) {
        return StatusUtilities.managerDependenciesCompleted(managedResource);
    }

    private Condition findConditionByType(String conditionType, T managedResource) {
        Optional<Condition> condition = managedResource.getConditions().stream().filter(x -> conditionType.equals(x.getType())).findFirst();

        // The conditions should be created/updated when the request from the user is accepted.
        return condition.orElseThrow(() -> {
            String message = String.format("Condition '%s' not found for resource '%s'.", conditionType, managedResource);
            LOGGER.error(message);
            return new IllegalStateException(message);
        });
    }
}
