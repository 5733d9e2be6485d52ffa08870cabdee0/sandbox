package com.redhat.service.bridge.rhoas.testprofiles;

import java.util.Collections;
import java.util.Map;

import com.redhat.service.bridge.rhoas.RhoasClient;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RhoasEnabledTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.singletonMap(RhoasClient.ENABLED_FLAG, "true");
    }
}
