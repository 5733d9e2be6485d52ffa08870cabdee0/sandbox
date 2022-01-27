package com.redhat.service.bridge.infra.utils;

public class InternalKafkaTopicNameBuilder {

    private static final String PREFIX = "ob-";

    public static String build(String bridgeId, boolean isDevEnv) {
        if (isDevEnv) {
            return "events";
        }
        return PREFIX + bridgeId;
    }

    public static String build(String bridgeId) {
        return build(bridgeId, true);
    }
}