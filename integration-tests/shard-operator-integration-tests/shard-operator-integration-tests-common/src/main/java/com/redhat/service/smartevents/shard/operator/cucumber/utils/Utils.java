package com.redhat.service.smartevents.shard.operator.cucumber.utils;

public class Utils {
    public static String getSystemProperty(String parameters) {
        if (System.getProperty(parameters).isEmpty()) {
            throw new RuntimeException("Property " + parameters + " was not defined.");
        }
        return System.getProperty(parameters);
    }
}
