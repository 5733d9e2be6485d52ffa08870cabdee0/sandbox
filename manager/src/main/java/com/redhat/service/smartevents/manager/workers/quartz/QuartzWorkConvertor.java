package com.redhat.service.smartevents.manager.workers.quartz;

import java.time.ZonedDateTime;

import org.quartz.JobDataMap;

import com.redhat.service.smartevents.manager.models.Work;

import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_ATTEMPTS;
import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_ID;
import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_SUBMITTED_AT;
import static com.redhat.service.smartevents.manager.workers.quartz.QuartzWorkManagerImpl.STATE_FIELD_TYPE;

public class QuartzWorkConvertor {

    private QuartzWorkConvertor() {
        //Utility class of public static members
    }

    // Create the JobDataMap that Quartz needs to construct a Trigger to handle the Work...
    public static JobDataMap convertToJobData(Work work) {
        JobDataMap jd = new JobDataMap();
        jd.put(STATE_FIELD_ID, work.getManagedResourceId());
        jd.put(STATE_FIELD_TYPE, work.getType());
        jd.putAsString(STATE_FIELD_ATTEMPTS, work.getAttempts());
        jd.put(STATE_FIELD_SUBMITTED_AT, work.getSubmittedAt().toString());
        return jd;
    }

    // Create the Work item that the Worker needs to handle from the Quartz JobDataMap...
    public static Work convertFromJobData(JobDataMap jobData) {
        Work work = new Work();
        work.setManagedResourceId(jobData.getString(STATE_FIELD_ID));
        work.setAttempts(jobData.getIntegerFromString(STATE_FIELD_ATTEMPTS));
        work.setSubmittedAt(ZonedDateTime.parse(jobData.getString(STATE_FIELD_SUBMITTED_AT)));
        work.setType(jobData.getString(STATE_FIELD_TYPE));
        return work;
    }

}
