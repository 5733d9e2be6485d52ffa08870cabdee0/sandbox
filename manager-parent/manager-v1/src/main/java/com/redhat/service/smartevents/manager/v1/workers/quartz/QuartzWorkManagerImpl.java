package com.redhat.service.smartevents.manager.v1.workers.quartz;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.v1.api.V1;
import com.redhat.service.smartevents.manager.core.models.ManagedResource;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;

import io.quarkus.runtime.Quarkus;

import static com.redhat.service.smartevents.manager.core.workers.quartz.QuartzWorkConvertor.convertToJobData;

@V1
@ApplicationScoped
public class QuartzWorkManagerImpl implements WorkManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzWorkManagerImpl.class);

    @ConfigProperty(name = "event-bridge.resources.workers.job-retry-interval-seconds")
    long jobScheduleInterval;

    @Inject
    org.quartz.Scheduler quartz;

    private JobDetail workJob;

    @PostConstruct
    public void init() {
        try {
            workJob = JobBuilder.newJob(WorkJob.class).storeDurably().build();
            quartz.addJob(workJob, false);
        } catch (SchedulerException e) {
            LOGGER.error("Unable to add Job(s) to Quartz.", e);
            Quarkus.asyncExit(1);
        }
    }

    @Override
    public Work schedule(ManagedResource managedResource) {
        Work work = Work.forResource(managedResource);
        doSchedule(work);
        return work;
    }

    @Override
    public void reschedule(Work work) {
        doReschedule(work);
    }

    @Override
    public void rescheduleAfterFailure(Work work) {
        work.setAttempts(work.getAttempts() + 1);
        doReschedule(work);
    }

    @Override
    public boolean exists(ManagedResource managedResource) {
        Work work = Work.forResource(managedResource);
        TriggerKey key = new TriggerKey(work.getManagedResourceId(), work.getType());
        try {
            return quartz.checkExists(key);
        } catch (SchedulerException e) {
            String message = "Failed to check if a Job exists for resource of type '" + work.getType() + "' and id '" + work.getManagedResourceId() + "'";
            throw new InternalPlatformException(message, e);
        }
    }

    // Schedule the first execution of a Job.
    // We cannot use Quartz scheduler to invoke the job repeatedly as we store mutable state within the
    // Trigger definition and Quartz does not support updating the Trigger JobDataMap after the Trigger
    // has been created. Rescheduling of the Job is handled by AbstractWorker if either an execution
    // failed or the Work remained incomplete.
    protected void doSchedule(Work work) {
        try {
            quartz.scheduleJob(makeTrigger(work));
        } catch (SchedulerException e) {
            String message = "Failed to schedule work for resource of type '" + work.getType() + "' and id '" + work.getManagedResourceId() + "'";
            throw new InternalPlatformException(message, e);
        }
    }

    protected void doReschedule(Work work) {
        TriggerKey key = new TriggerKey(work.getManagedResourceId(), work.getType());
        try {
            quartz.rescheduleJob(key, makeTrigger(work));
        } catch (SchedulerException e) {
            String message = "Failed to reschedule work for resource of type '" + work.getType() + "' and id '" + work.getManagedResourceId() + "'";
            throw new InternalPlatformException(message, e);
        }
    }

    private Trigger makeTrigger(Work work) {
        JobDataMap jobData = convertToJobData(work);
        TriggerKey key = new TriggerKey(work.getManagedResourceId(), work.getType());
        Date jobExecutionTime = Date.from(ZonedDateTime.now().plus(this.jobScheduleInterval, ChronoUnit.SECONDS).toInstant());

        return TriggerBuilder.newTrigger()
                .forJob(workJob)
                .withIdentity(key)
                .usingJobData(jobData)
                .startAt(jobExecutionTime)
                .build();
    }
}
