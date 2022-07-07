package com.redhat.service.smartevents.manager.workers;

import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;

public interface WorkManager {

    String MANAGED_RESOURCES_GROUP = "ManagedResourcesTriggers";

    // Whilst Quartz supports use of Serialisable objects as JobData, Quarkus configures Quartz to force
    // use of String keys and values when using a JDBC JobStore. Therefore, these properties are always
    // stored as Strings and the serialization/de-serialisation handled by RHOSE.
    // See https://quarkusio.zulipchat.com/#narrow/stream/187030-users/topic/Quartz.3A.20JDBC.20JobStore.3A.20useProperties
    String STATE_FIELD_ID = "id";
    String STATE_FIELD_ATTEMPTS = "attempts";
    String STATE_FIELD_SUBMITTED_AT = "submittedAt";

    void schedule(Bridge bridge);

    void schedule(Processor processor);

}