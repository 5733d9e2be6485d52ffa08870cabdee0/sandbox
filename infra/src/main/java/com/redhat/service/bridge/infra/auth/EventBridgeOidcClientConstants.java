package com.redhat.service.bridge.infra.auth;

import java.time.Duration;

public class EventBridgeOidcClientConstants {
    public static final String SCHEDULER_TIME = "5s";
    public static final Duration SSO_CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration REFRESH_TOKEN_TIME_SKEW = Duration.ofSeconds(30);
}
