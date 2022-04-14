package com.redhat.service.smartevents.test.rhoas.testprofiles;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RhoasWithMissingInstanceApiPropsTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>();

        config.put("event-bridge.rhoas.mgmt-api.host", "${rhoas-mock-server.url:}${rhoas-mock-server.mgmt-api.base-path}");

        config.put("event-bridge.rhoas.sso.red-hat.auth-server-url", "${rhoas-mock-server.url:}${rhoas-mock-server.sso.red-hat.base-path}");
        config.put("event-bridge.rhoas.sso.red-hat.client-id", "${rhoas-mock-server.sso.red-hat.client-id}");
        config.put("event-bridge.rhoas.sso.red-hat.refresh-token", "${rhoas-mock-server.sso.red-hat.refresh-token}");

        config.put("quarkus.oidc-client.red-hat-sso.discovery-enabled", "false");
        config.put("quarkus.oidc-client.red-hat-sso.token-path", "/token");
        config.put("quarkus.oidc-client.mas-sso.discovery-enabled", "false");
        config.put("quarkus.oidc-client.mas-sso.token-path", "/token");

        return config;
    }

}
