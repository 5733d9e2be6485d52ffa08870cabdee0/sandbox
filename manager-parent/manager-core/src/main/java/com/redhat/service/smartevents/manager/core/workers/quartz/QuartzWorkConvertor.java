package com.redhat.service.smartevents.manager.core.workers.quartz;

import java.time.ZonedDateTime;

import org.quartz.JobDataMap;

import com.redhat.service.smartevents.manager.core.workers.Work;

public class QuartzWorkConvertor {

    // Whilst Quartz supports use of Serialisable objects as JobData, Quarkus configures Quartz to force
    // use of String keys and values when using a JDBC JobStore. Therefore, these properties are always
    // stored as Strings and the serialization/de-serialisation handled by RHOSE.
    // See https://quarkusio.zulipchat.com/#narrow/stream/187030-users/topic/Quartz.3A.20JDBC.20JobStore.3A.20useProperties
    public static final String STATE_FIELD_ID = "id";
    public static final String STATE_FIELD_ATTEMPTS = "attempts";
    public static final String STATE_FIELD_SUBMITTED_AT = "submittedAt";
    public static final String STATE_FIELD_TYPE = "type";

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
