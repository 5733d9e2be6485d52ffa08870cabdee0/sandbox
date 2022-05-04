package com.redhat.service.smartevents.processor;

public class ValidatorUtils {

    public static String notValidKey(String key) {
        return "The supplied " + key + " parameter is not valid";
    }
}
