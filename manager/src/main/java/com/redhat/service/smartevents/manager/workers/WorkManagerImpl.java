package com.redhat.service.smartevents.manager.workers;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.workers.resources.BridgeWorker;
import com.redhat.service.smartevents.manager.workers.resources.ProcessorWorker;

import io.quarkus.runtime.Quarkus;

@ApplicationScoped
public class WorkManagerImpl implements WorkManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkManagerImpl.class);

    @Inject
    org.quartz.Scheduler quartz;

    private JobDetail bridgeWorkerJob;
    private JobDetail processorWorkerJob;

    @PostConstruct
    public void init() {
        try {
            bridgeWorkerJob = JobBuilder.newJob(BridgeWorker.class).storeDurably().build();
            processorWorkerJob = JobBuilder.newJob(ProcessorWorker.class).storeDurably().build();
            quartz.addJob(bridgeWorkerJob, false);
            quartz.addJob(processorWorkerJob, false);
        } catch (SchedulerException e) {
            LOGGER.error("Unable to add Job(s) to Quartz.", e);
            Quarkus.asyncExit(1);
        }
    }

    @Override
    public void schedule(Bridge bridge) {
        doSchedule(bridge.getId(), bridgeWorkerJob);
    }

    @Override
    public void schedule(Processor processor) {
        doSchedule(processor.getId(), processorWorkerJob);
    }

    // Schedule the first execution of a Job.
    // We cannot use Quartz scheduler to invoke the job repeatedly as we store mutable state within the
    // Trigger definition and Quartz does not support updating the Trigger JobDataMap after the Trigger
    // has been created. Rescheduling of the Job is handled by AbstractWorker if either an execution
    // failed or the Work remained incomplete.
    protected void doSchedule(String id, JobDetail jobDetail) {
        try {
            TriggerKey key = new TriggerKey(id, MANAGED_RESOURCES_GROUP);
            if (Objects.nonNull(quartz.getTrigger(key))) {
                LOGGER.info(String.format("Work already exists for '%s'. Skipping.", id));
            }

            // We have to store the serialised JobData due to limitations in Quarkus. See WorkManager for details.
            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity(key)
                    .usingJobData(STATE_FIELD_ID, id)
                    .usingJobData(STATE_FIELD_ATTEMPTS, String.valueOf(0))
                    .usingJobData(STATE_FIELD_SUBMITTED_AT, ZonedDateTime.now(ZoneOffset.UTC).toString())
                    .startNow()
                    .build();
            quartz.scheduleJob(trigger);

        } catch (SchedulerException e) {
            LOGGER.info(String.format("Unable to schedule work for '%s'", id), e);
        }
    }
}
