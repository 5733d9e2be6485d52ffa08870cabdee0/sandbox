package com.redhat.service.smartevents.manager.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import com.redhat.service.smartevents.manager.models.ManagedResource;

import static com.redhat.service.smartevents.manager.workers.WorkManager.STATE_FIELD_ATTEMPTS;
import static com.redhat.service.smartevents.manager.workers.WorkManager.STATE_FIELD_ID;
import static com.redhat.service.smartevents.manager.workers.WorkManager.STATE_FIELD_SUBMITTED_AT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkerTestUtils {

    private WorkerTestUtils() {
        //Static utility methods
    }

    static JobExecutionContext makeJobExecutionContext(ManagedResource resource) {
        return makeJobExecutionContext(resource.getId(), 0L, ZonedDateTime.now(ZoneOffset.UTC));
    }

    static JobExecutionContext makeJobExecutionContext(ManagedResource resource, long attempts) {
        return makeJobExecutionContext(resource.getId(), attempts, ZonedDateTime.now(ZoneOffset.UTC));
    }

    static JobExecutionContext makeJobExecutionContext(ManagedResource resource, ZonedDateTime submittedAt) {
        return makeJobExecutionContext(resource.getId(), 0L, submittedAt);
    }

    static JobExecutionContext makeJobExecutionContext(String id, long attempts, ZonedDateTime submittedAt) {
        JobDataMap data = new JobDataMap();
        data.put(STATE_FIELD_ID, id);
        data.put(STATE_FIELD_ATTEMPTS, attempts);
        data.put(STATE_FIELD_SUBMITTED_AT, submittedAt);

        JobExecutionContext context = mock(JobExecutionContext.class);
        Trigger trigger = mock(Trigger.class);
        when(context.getTrigger()).thenReturn(trigger);
        when(trigger.getJobDataMap()).thenReturn(data);
        when(trigger.getKey()).thenReturn(new TriggerKey(id));

        return context;
    }

}
