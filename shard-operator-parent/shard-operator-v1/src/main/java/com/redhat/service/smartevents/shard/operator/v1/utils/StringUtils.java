package com.redhat.service.smartevents.shard.operator.v1.utils;

public class StringUtils {

        public static boolean stringIsNullOrEmpty(String string) {
            return string == null || string.isEmpty();
        }
        public static String emptyToNull(String string) {
            return stringIsNullOrEmpty(string) ? null : string;
        }
    }


