package com.redhat.service.bridge.manager.config;

import org.eclipse.microprofile.config.ConfigProvider;

public class ConfigUtils {

    public static final String TOPIC_PREFIX_VARIABLE = "rhoas.topic-prefix";
    public static final String TOPIC_PREFIX_DEFAULT = "ob-";

    public static String topicPrefix() {
        return ConfigProvider.getConfig()
                .getOptionalValue(TOPIC_PREFIX_VARIABLE, String.class)
                .orElse(TOPIC_PREFIX_DEFAULT);
    }

    private ConfigUtils() {
    }

}
