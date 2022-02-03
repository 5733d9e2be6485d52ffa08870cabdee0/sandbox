package com.redhat.service.bridge.test.rhoas.testprofiles;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RhoasEnabledWithMissingMgmtApiPropsTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>();

        config.put("event-bridge.rhoas.instance-api.host", "${rhoas-mock-server.url:}${rhoas-mock-server.instance-api.base-path}/rest");

        config.put("event-bridge.rhoas.sso.mas.auth-server-url", "${rhoas-mock-server.url:}${rhoas-mock-server.sso.mas.base-path}");
        config.put("event-bridge.rhoas.sso.mas.client-id", "${rhoas-mock-server.sso.mas.client-id}");
        config.put("event-bridge.rhoas.sso.mas.client-secret", "${rhoas-mock-server.sso.mas.client-secret}");

        config.put("quarkus.oidc-client.red-hat-sso.discovery-enabled", "false");
        config.put("quarkus.oidc-client.red-hat-sso.token-path", "/token");
        config.put("quarkus.oidc-client.mas-sso.discovery-enabled", "false");
        config.put("quarkus.oidc-client.mas-sso.token-path", "/token");

        return config;
    }
}
