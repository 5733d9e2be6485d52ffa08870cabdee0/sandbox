package com.redhat.service.smartevents.manager.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.redhat.service.smartevents.manager.models.ManagedResource;
import com.redhat.service.smartevents.manager.workers.Work;

class WorkerTestUtils {

    private WorkerTestUtils() {
        //Static utility methods
    }

    static Work makeWork(ManagedResource resource) {
        return makeWork(resource.getId(), 0L, ZonedDateTime.now(ZoneOffset.UTC));
    }

    static Work makeWork(ManagedResource resource, long attempts) {
        return makeWork(resource.getId(), attempts, ZonedDateTime.now(ZoneOffset.UTC));
    }

    static Work makeWork(ManagedResource resource, ZonedDateTime submittedAt) {
        return makeWork(resource.getId(), 0L, submittedAt);
    }

    static Work makeWork(String id, long attempts, ZonedDateTime submittedAt) {
        Work work = new Work();
        work.setManagedResourceId(id);
        work.setType("TEST");
        work.setAttempts(attempts);
        work.setSubmittedAt(submittedAt);
        return work;
    }

}
