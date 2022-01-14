package com.redhat.service.bridge.test.rhoas.testprofiles;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RhoasEnabledWithMissingPropsTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>();

        config.put("event-bridge.feature-flags.rhoas-enabled", "true");

        config.put("quarkus.oidc-client.red-hat-sso.discovery-enabled", "false");
        config.put("quarkus.oidc-client.red-hat-sso.token-path", "/token");
        config.put("quarkus.oidc-client.mas-sso.discovery-enabled", "false");
        config.put("quarkus.oidc-client.mas-sso.token-path", "/token");

        return config;
    }
}
