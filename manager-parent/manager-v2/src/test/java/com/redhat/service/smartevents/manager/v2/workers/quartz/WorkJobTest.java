package com.redhat.service.smartevents.manager.v2.workers.quartz;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import com.redhat.service.smartevents.manager.core.models.ManagedResource;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.core.workers.Worker;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.workers.resources.BridgeWorker;

import static com.redhat.service.smartevents.manager.core.workers.quartz.QuartzWorkConvertor.STATE_FIELD_ATTEMPTS;
import static com.redhat.service.smartevents.manager.core.workers.quartz.QuartzWorkConvertor.STATE_FIELD_ID;
import static com.redhat.service.smartevents.manager.core.workers.quartz.QuartzWorkConvertor.STATE_FIELD_SUBMITTED_AT;
import static com.redhat.service.smartevents.manager.core.workers.quartz.QuartzWorkConvertor.STATE_FIELD_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkJobTest {

    @Mock
    BridgeWorker bridgeWorker;

    @Mock
    JobExecutionContext context;

    @Captor
    ArgumentCaptor<Work> workArgumentCaptor;

    WorkJob workJob;

    @BeforeEach
    public void setup() {
        workJob = new WorkJob();
        workJob.bridgeWorker = bridgeWorker;
    }

    @Test
    void testExecutesBridge() {
        assertExecutes(Bridge.class, bridgeWorker);
    }

    void assertExecutes(Class<? extends ManagedResource> managedResourceClass, Worker<?> worker) {
        JobDataMap jobDataMap = stubJobDataMap(managedResourceClass);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);

        workJob.execute(context);

        verify(worker).handleWork(workArgumentCaptor.capture());

        Work work = workArgumentCaptor.getValue();

        assertThat(work.getManagedResourceId()).isEqualTo("id");
        assertThat(work.getType()).isEqualTo(managedResourceClass.getName());
        assertThat(work.getSubmittedAt().toString()).isEqualTo(jobDataMap.getString(STATE_FIELD_SUBMITTED_AT));
        assertThat(work.getAttempts()).isEqualTo(0L);
    }

    private JobDataMap stubJobDataMap(Class<? extends ManagedResource> managedResourceClass) {
        ZonedDateTime submittedAt = ZonedDateTime.now(ZoneOffset.UTC);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(STATE_FIELD_ID, "id");
        jobDataMap.putAsString(STATE_FIELD_ATTEMPTS, 0);
        jobDataMap.put(STATE_FIELD_SUBMITTED_AT, submittedAt.toString());
        jobDataMap.put(STATE_FIELD_TYPE, managedResourceClass.getName());

        return jobDataMap;
    }
}
