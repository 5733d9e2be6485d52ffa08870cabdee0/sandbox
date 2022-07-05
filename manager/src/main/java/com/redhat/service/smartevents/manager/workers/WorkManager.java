package com.redhat.service.smartevents.manager.workers;

import org.quartz.JobDataMap;

import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;

public interface WorkManager {

    String MANAGED_RESOURCES_GROUP = "ManagedResourcesTriggers";

    String STATE_FIELD_ID = "id";

    String STATE_FIELD_ATTEMPTS = "attempts";

    String STATE_FIELD_SUBMITTED_AT = "submittedAt";

    JobDataMap schedule(Bridge bridge);

    JobDataMap schedule(Processor processor);

}