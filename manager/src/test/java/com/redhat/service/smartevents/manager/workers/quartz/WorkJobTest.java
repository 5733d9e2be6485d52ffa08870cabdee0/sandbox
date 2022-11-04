package com.redhat.service.smartevents.manager.workers.quartz;

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

import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.manager.models.ManagedResource;
import com.redhat.service.smartevents.manager.persistence.v1.models.Bridge;
import com.redhat.service.smartevents.manager.persistence.v1.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.persistence.v1.models.Processor;
import com.redhat.service.smartevents.manager.workers.Work;
import com.redhat.service.smartevents.manager.workers.Worker;
import com.redhat.service.smartevents.manager.workers.resources.v1.BridgeWorker;
import com.redhat.service.smartevents.manager.workers.resources.v1.ProcessorWorker;

import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_ATTEMPTS;
import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_ID;
import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_SUBMITTED_AT;
import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkJobTest {

    @Mock
    ProcessorWorker processorWorker;

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
        workJob.processorWorker = processorWorker;
    }

    @Test
    void testExecutesBridge() {
        assertExecutes(Bridge.class, bridgeWorker);
    }

    @Test
    void testExecutesProcessor() {
        assertExecutes(Processor.class, processorWorker);
    }

    @Test
    void testExecutesConnector() {
        JobDataMap jobDataMap = stubJobDataMap(ConnectorEntity.class);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);

        assertThatThrownBy(() -> workJob.execute(context)).isInstanceOf(InternalPlatformException.class);
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
