package com.redhat.service.smartevents.manager.workers.quartz;

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
import com.redhat.service.smartevents.manager.models.Work;

import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_ATTEMPTS;
import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_ID;
import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_SUBMITTED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuartzWorkManagerImplTest {

    private static final String RESOURCE_ID = "123";

    @Mock
    Scheduler quartz;

    @Mock
    Processor resource;

    @Captor
    ArgumentCaptor<Trigger> triggerArgumentCaptor;

    private QuartzWorkManagerImpl manager;

    @BeforeEach
    protected void setup() {
        this.manager = new QuartzWorkManagerImpl();
        this.manager.quartz = quartz;
        this.manager.init();

        when(resource.getId()).thenReturn(RESOURCE_ID);
    }

    @Test
    void scheduleDoesNotRescheduleForExistingWork() throws Exception {
        Work work = manager.schedule(resource);

        verify(quartz).scheduleJob(triggerArgumentCaptor.capture());

        Trigger trigger = triggerArgumentCaptor.getValue();
        assertThat(trigger.getKey().getName()).isEqualTo(RESOURCE_ID);
        assertThat(trigger.getKey().getGroup()).isEqualTo(work.getType());
        assertThat(trigger.getJobKey()).isNotNull();

        JobDataMap data = trigger.getJobDataMap();
        assertThat(data.getString(STATE_FIELD_ID)).isEqualTo(RESOURCE_ID);
        assertThat(data.getLong(STATE_FIELD_ATTEMPTS)).isZero();
        assertThat(data.get(STATE_FIELD_SUBMITTED_AT)).isNotNull();
    }

}
