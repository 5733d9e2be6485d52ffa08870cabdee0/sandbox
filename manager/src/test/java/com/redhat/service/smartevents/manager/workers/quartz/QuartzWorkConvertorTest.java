package com.redhat.service.smartevents.manager.workers.quartz;

import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;

import com.redhat.service.smartevents.manager.models.ManagedResource;
import com.redhat.service.smartevents.manager.models.Work;
import com.redhat.service.smartevents.manager.utils.Fixtures;

import static org.assertj.core.api.Assertions.assertThat;

public class QuartzWorkConvertorTest {

    @Test
    void testWorkToJobDataMap() {
        ManagedResource bridge = Fixtures.createBridge();
        Work work = Work.forResource(bridge);
        work.setAttempts(1);

        JobDataMap jobDataMap = QuartzWorkConvertor.convertToJobData(work);

        assertThat(jobDataMap.containsKey(QuartzWorkManagerImpl.STATE_FIELD_ID)).isTrue();
        assertThat(jobDataMap.containsKey(QuartzWorkManagerImpl.STATE_FIELD_TYPE)).isTrue();
        assertThat(jobDataMap.containsKey(QuartzWorkManagerImpl.STATE_FIELD_SUBMITTED_AT)).isTrue();
        assertThat(jobDataMap.containsKey(QuartzWorkManagerImpl.STATE_FIELD_ATTEMPTS)).isTrue();

        assertThat(jobDataMap.getString(QuartzWorkManagerImpl.STATE_FIELD_ID)).isEqualTo(work.getManagedResourceId());
        assertThat(jobDataMap.getString(QuartzWorkManagerImpl.STATE_FIELD_TYPE)).isEqualTo(work.getType());
        assertThat(jobDataMap.getString(QuartzWorkManagerImpl.STATE_FIELD_SUBMITTED_AT)).isEqualTo(work.getSubmittedAt().toString());
        assertThat(jobDataMap.getString(QuartzWorkManagerImpl.STATE_FIELD_ATTEMPTS)).isEqualTo(String.valueOf(work.getAttempts()));
    }

    @Test
    void testJobDataMapToWork() {
        ManagedResource bridge = Fixtures.createBridge();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(QuartzWorkManagerImpl.STATE_FIELD_ID, bridge.getId());
        jobDataMap.put(QuartzWorkManagerImpl.STATE_FIELD_TYPE, bridge.getClass().getName());
        jobDataMap.put(QuartzWorkManagerImpl.STATE_FIELD_SUBMITTED_AT, bridge.getSubmittedAt().toString());
        jobDataMap.put(QuartzWorkManagerImpl.STATE_FIELD_ATTEMPTS, "1");

        Work work = QuartzWorkConvertor.convertFromJobData(jobDataMap);

        assertThat(work.getManagedResourceId()).isEqualTo(jobDataMap.getString(QuartzWorkManagerImpl.STATE_FIELD_ID));
        assertThat(work.getType()).isEqualTo(jobDataMap.getString(QuartzWorkManagerImpl.STATE_FIELD_TYPE));
        assertThat(work.getSubmittedAt().toString()).isEqualTo(jobDataMap.getString(QuartzWorkManagerImpl.STATE_FIELD_SUBMITTED_AT));
        assertThat(work.getAttempts()).isEqualTo(Long.valueOf(jobDataMap.getString(QuartzWorkManagerImpl.STATE_FIELD_ATTEMPTS)));
    }

}
