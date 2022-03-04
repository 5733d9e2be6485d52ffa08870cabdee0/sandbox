package com.redhat.service.bridge.integration.tests.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Utils {

    public static String getSystemProperty(String parameters) {
        if (System.getProperty(parameters).isEmpty()) {
            throw new RuntimeException("Property " + parameters + " was not defined.");
        }
        return System.getProperty(parameters);
    }

    public static String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 4);
    }

    public static String getTodayDateTime() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return date.format(formatter);
    }
}
