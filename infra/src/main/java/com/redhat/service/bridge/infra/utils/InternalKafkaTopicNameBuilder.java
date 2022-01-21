package com.redhat.service.bridge.infra.utils;

public class InternalKafkaTopicNameBuilder {

    private static final String PREFIX = "ob-";

    public static String build(String bridgeId) {
        return PREFIX + bridgeId;
    }
}
