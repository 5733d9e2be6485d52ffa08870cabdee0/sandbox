package com.redhat.service.smartevents.manager.workers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.Trigger;

import com.redhat.service.smartevents.manager.models.Processor;

import static com.redhat.service.smartevents.manager.workers.WorkManager.MANAGED_RESOURCES_GROUP;
import static com.redhat.service.smartevents.manager.workers.WorkManager.STATE_FIELD_ATTEMPTS;
import static com.redhat.service.smartevents.manager.workers.WorkManager.STATE_FIELD_ID;
import static com.redhat.service.smartevents.manager.workers.WorkManager.STATE_FIELD_SUBMITTED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkManagerImplTest {

    private static final String RESOURCE_ID = "123";

    @Mock
    Scheduler quartz;

    @Mock
    Processor resource;

    @Captor
    ArgumentCaptor<Trigger> triggerArgumentCaptor;

    private WorkManagerImpl manager;

    @BeforeEach
    protected void setup() {
        this.manager = new WorkManagerImpl();
        this.manager.quartz = quartz;
        this.manager.init();

        when(resource.getId()).thenReturn(RESOURCE_ID);
    }

    @Test
    void scheduleForNewWork() throws Exception {
        manager.schedule(resource);

        verify(quartz).scheduleJob(any());
    }

    @Test
    void scheduleDoesNotRescheduleForExistingWork() throws Exception {
        Trigger existingTrigger = mock(Trigger.class);
        when(quartz.getTrigger(any())).thenReturn(existingTrigger);

        manager.schedule(resource);

        verify(quartz).scheduleJob(triggerArgumentCaptor.capture());

        Trigger trigger = triggerArgumentCaptor.getValue();
        assertThat(trigger.getKey().getName()).isEqualTo(RESOURCE_ID);
        assertThat(trigger.getKey().getGroup()).isEqualTo(MANAGED_RESOURCES_GROUP);
        assertThat(trigger.getJobKey()).isNotNull();

        JobDataMap data = trigger.getJobDataMap();
        assertThat(data.getString(STATE_FIELD_ID)).isEqualTo(RESOURCE_ID);
        assertThat(data.getLong(STATE_FIELD_ATTEMPTS)).isEqualTo(0);
        assertThat(data.get(STATE_FIELD_SUBMITTED_AT)).isNotNull();
    }

}
