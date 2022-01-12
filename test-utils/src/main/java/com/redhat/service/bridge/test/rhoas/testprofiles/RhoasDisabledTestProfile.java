package com.redhat.service.bridge.test.rhoas.testprofiles;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RhoasDisabledTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.singletonMap("event-bridge.feature-flags.rhoas-enabled", "false");
    }
}
