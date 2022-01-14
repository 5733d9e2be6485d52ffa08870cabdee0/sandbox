package com.redhat.service.bridge.rhoas;

public class RhoasProperties {

    public static final String ENABLED_FLAG = "event-bridge.feature-flags.rhoas-enabled";
    public static final String ENABLED_FLAG_DEFAULT_VALUE = "false";

    public static final String INSTANCE_API_HOST = "event-bridge.rhoas.instance-api.host";
    public static final String MGMT_API_HOST = "event-bridge.rhoas.mgmt-api.host";
    public static final String SSO_RED_HAT_REFRESH_TOKEN = "event-bridge.rhoas.sso.red-hat.refresh-token";

    private RhoasProperties() {
    }
}
