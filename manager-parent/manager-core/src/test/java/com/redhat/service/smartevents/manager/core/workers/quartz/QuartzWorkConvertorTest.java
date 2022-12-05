package com.redhat.service.smartevents.manager.core.workers.quartz;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;

import com.redhat.service.smartevents.manager.core.mocks.ManagedResourceForTests;
import com.redhat.service.smartevents.manager.core.models.ManagedResource;
import com.redhat.service.smartevents.manager.core.workers.Work;

import static org.assertj.core.api.Assertions.assertThat;

public class QuartzWorkConvertorTest {

    @Test
    void testWorkToJobDataMap() {
        ManagedResource resource = new ManagedResourceForTests();
        Work work = Work.forResource(resource);
        work.setAttempts(1);

        JobDataMap jobDataMap = QuartzWorkConvertor.convertToJobData(work);

        assertThat(jobDataMap.containsKey(QuartzWorkConvertor.STATE_FIELD_ID)).isTrue();
        assertThat(jobDataMap.containsKey(QuartzWorkConvertor.STATE_FIELD_TYPE)).isTrue();
        assertThat(jobDataMap.containsKey(QuartzWorkConvertor.STATE_FIELD_SUBMITTED_AT)).isTrue();
        assertThat(jobDataMap.containsKey(QuartzWorkConvertor.STATE_FIELD_ATTEMPTS)).isTrue();

        assertThat(jobDataMap.getString(QuartzWorkConvertor.STATE_FIELD_ID)).isEqualTo(work.getManagedResourceId());
        assertThat(jobDataMap.getString(QuartzWorkConvertor.STATE_FIELD_TYPE)).isEqualTo(work.getType());
        assertThat(jobDataMap.getString(QuartzWorkConvertor.STATE_FIELD_SUBMITTED_AT)).isEqualTo(work.getSubmittedAt().toString());
        assertThat(jobDataMap.getString(QuartzWorkConvertor.STATE_FIELD_ATTEMPTS)).isEqualTo(String.valueOf(work.getAttempts()));
    }

    @Test
    void testJobDataMapToWork() {
        ManagedResource resource = new ManagedResourceForTests();
        resource.setId("id");
        resource.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(QuartzWorkConvertor.STATE_FIELD_ID, resource.getId());
        jobDataMap.put(QuartzWorkConvertor.STATE_FIELD_TYPE, resource.getClass().getName());
        jobDataMap.put(QuartzWorkConvertor.STATE_FIELD_SUBMITTED_AT, resource.getSubmittedAt().toString());
        jobDataMap.put(QuartzWorkConvertor.STATE_FIELD_ATTEMPTS, "1");

        Work work = QuartzWorkConvertor.convertFromJobData(jobDataMap);

        assertThat(work.getManagedResourceId()).isEqualTo(jobDataMap.getString(QuartzWorkConvertor.STATE_FIELD_ID));
        assertThat(work.getType()).isEqualTo(jobDataMap.getString(QuartzWorkConvertor.STATE_FIELD_TYPE));
        assertThat(work.getSubmittedAt().toString()).isEqualTo(jobDataMap.getString(QuartzWorkConvertor.STATE_FIELD_SUBMITTED_AT));
        assertThat(work.getAttempts()).isEqualTo(Long.valueOf(jobDataMap.getString(QuartzWorkConvertor.STATE_FIELD_ATTEMPTS)));
    }
}
