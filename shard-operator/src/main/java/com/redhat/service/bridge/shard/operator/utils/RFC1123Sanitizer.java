package com.redhat.service.bridge.shard.operator.utils;

public class RFC1123Sanitizer {
    public static String sanitize(String s) {
        // TODO: Implement good strategy for sanification https://issues.redhat.com/browse/MGDOBR-100
        return s.toLowerCase();
    }
}
