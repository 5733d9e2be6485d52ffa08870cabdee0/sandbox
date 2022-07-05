package com.redhat.service.smartevents.manager.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.models.ManagedResource;
import com.redhat.service.smartevents.manager.workers.Worker;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.ACCEPTED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DELETED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DELETING;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DEPROVISION;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.PREPARING;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.workers.WorkManager.STATE_FIELD_ATTEMPTS;
import static com.redhat.service.smartevents.manager.workers.WorkManager.STATE_FIELD_ID;
import static com.redhat.service.smartevents.manager.workers.WorkManager.STATE_FIELD_SUBMITTED_AT;

public abstract class AbstractWorker<T extends ManagedResource> implements Job, Worker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorker.class);

    private static final Set<ManagedResourceStatus> PROVISIONING_STARTED = Set.of(ACCEPTED, PREPARING);
    private static final Set<ManagedResourceStatus> DEPROVISIONING_STARTED = Set.of(DEPROVISION, DELETING);

    protected static final Set<ManagedResourceStatus> PROVISIONING_COMPLETED = Set.of(READY, FAILED);
    protected static final Set<ManagedResourceStatus> DEPROVISIONING_COMPLETED = Set.of(DELETED, FAILED);

    @ConfigProperty(name = "event-bridge.resources.worker.max-retries")
    int maxRetries;

    @ConfigProperty(name = "event-bridge.resources.workers.timeout-seconds")
    int timeoutSeconds;

    @ConfigProperty(name = "event-bridge.resources.workers.frequency")
    String frequencyConfigProperty;
    long frequency;

    @Inject
    protected Scheduler quartz;

    @PostConstruct
    protected void init() {
        try {
            frequency = Long.parseLong(frequencyConfigProperty);
        } catch (NumberFormatException nfe) {
            LOGGER.warn("Unable to parse Config Property 'event-bridge.resources.workers.frequency' of '{}'. Using default of 30s.",
                    frequencyConfigProperty);
            frequency = 30;
        }
    }

    @Override
    public void execute(JobExecutionContext context) {
        handleWork(context);
    }

    @Override
    public T handleWork(JobExecutionContext context) {
        String id = getId(context);
        T managedResource = load(id);
        if (Objects.isNull(managedResource)) {
            //Work has been scheduled but cannot be found. Something (horribly) wrong has happened.
            throw new IllegalStateException(String.format("Resource with id '%s' no longer exists in the database.", id));
        }

        // Fail when we've had enough
        if (areRetriesExceeded(context, managedResource) || isTimeoutExceeded(context, managedResource)) {
            managedResource.setStatus(FAILED);
            persist(managedResource);
            return managedResource;
        }

        boolean complete = false;
        T updated = managedResource;
        if (PROVISIONING_STARTED.contains(managedResource.getStatus())) {
            try {
                updated = createDependencies(context, managedResource);
                complete = isProvisioningComplete(updated);
            } catch (Exception e) {
                LOGGER.error(String.format("Failed to create dependencies for '%s'.", id), e);
                // Something has gone wrong. We need to retry.
                recordAttemptAndReschedule(context);
                return updated;
            }
        } else if (DEPROVISIONING_STARTED.contains(managedResource.getStatus())) {
            try {
                updated = deleteDependencies(context, managedResource);
                complete = isDeprovisioningComplete(updated);
            } catch (Exception e) {
                LOGGER.info(String.format("Failed to delete dependencies for '%s'.", id), e);
                // Something has gone wrong. We need to retry.
                recordAttemptAndReschedule(context);
                return updated;
            }
        }

        if (!complete) {
            reschedule(context);
        }

        return updated;
    }

    protected abstract String getId(JobExecutionContext context);

    @Transactional
    protected T load(String id) {
        return getDao().findById(id);
    }

    @Transactional
    protected T persist(T managedResource) {
        return getDao().getEntityManager().merge(managedResource);
    }

    protected boolean areRetriesExceeded(JobExecutionContext context, ManagedResource managedResource) {
        JobDataMap data = context.getTrigger().getJobDataMap();
        long attempts = data.getLong(STATE_FIELD_ATTEMPTS);
        boolean areRetriesExceeded = attempts > maxRetries;
        if (areRetriesExceeded) {
            LOGGER.error(
                    "Max retry attempts exceeded trying to create dependencies for '{}' [{}].",
                    managedResource.getName(),
                    managedResource.getId());
        }
        return areRetriesExceeded;
    }

    protected boolean isTimeoutExceeded(JobExecutionContext context, ManagedResource managedResource) {
        JobDataMap data = context.getTrigger().getJobDataMap();
        ZonedDateTime submittedAt = (ZonedDateTime) data.get(STATE_FIELD_SUBMITTED_AT);
        boolean isTimeoutExceeded = ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(timeoutSeconds).isAfter(submittedAt);
        if (isTimeoutExceeded) {
            LOGGER.error(
                    "Timeout exceeded trying to create dependencies for '{}' [{}].",
                    managedResource.getName(),
                    managedResource.getId());
        }
        return isTimeoutExceeded;
    }

    protected abstract PanacheRepositoryBase<T, String> getDao();

    protected abstract T createDependencies(JobExecutionContext context, T managedResource);

    protected abstract T deleteDependencies(JobExecutionContext context, T managedResource);

    // When Work is "complete" the Work is removed from the Work Queue acted on by WorkManager.
    // For simple two-step chains (e.g. our existing Processor->Connector resource) it does not matter
    // a great deal if either Processor or Connector decide the work is complete. However, consider a three-step
    // chain: A->B->C where A is the primary work, B a dependency on A and C a dependency on B. We would not want
    // the work for A to be removed from the Work Queue until B and C are complete. Therefore, neither B nor C should
    // flag that work is complete. B would check on the status of C and A on B. These methods allow for this.
    protected abstract boolean isProvisioningComplete(T managedResource);

    protected abstract boolean isDeprovisioningComplete(T managedResource);

    protected void recordAttemptAndReschedule(JobExecutionContext context) {
        JobDataMap data = context.getTrigger().getJobDataMap();
        long attempts = data.getLong(STATE_FIELD_ATTEMPTS);
        data.put(STATE_FIELD_ATTEMPTS, attempts + 1);

        reschedule(context);
    }

    protected void reschedule(JobExecutionContext context) {
        try {
            Trigger existingTrigger = context.getTrigger();
            TriggerKey existingTriggerKey = existingTrigger.getKey();
            Trigger newTrigger = TriggerBuilder.newTrigger()
                    .forJob(existingTrigger.getJobKey())
                    .withIdentity(existingTriggerKey)
                    .usingJobData(existingTrigger.getJobDataMap())
                    .startAt(new Date(System.currentTimeMillis() + frequency * 1000))
                    .build();
            quartz.rescheduleJob(existingTriggerKey, newTrigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Unable to reschedule Job for '%s", context.getTrigger().getJobDataMap().getString(STATE_FIELD_ID)), e);
        }
    }

}
