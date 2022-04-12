package com.redhat.service.smartevents.manager;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * A Quarkus Test Profile that can be used to enable Quarkus's Scheduler Service for tests requiring Workers.
 */
public class WorkerSchedulerProfile implements QuarkusTestProfile {

    private static final Map<String, String> OVERRIDES = Map.of("quarkus.scheduler.enabled", "true",
            "event-bridge.resources.workers.schedule", "* * * * * ?");

    @Override
    public Map<String, String> getConfigOverrides() {
        return OVERRIDES;
    }
}
