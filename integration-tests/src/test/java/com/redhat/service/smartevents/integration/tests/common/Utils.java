package com.redhat.service.smartevents.integration.tests.common;

import java.util.UUID;

public class Utils {

    public static String getSystemProperty(String parameters) {
        if (System.getProperty(parameters) == null || System.getProperty(parameters).isEmpty()) {
            throw new RuntimeException("Property " + parameters + " was not defined.");
        }
        return System.getProperty(parameters);
    }

    public static String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 4);
    }
}
