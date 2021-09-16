package com.redhat.service.bridge.infra.filters;

public class FilterFactory {
    public static Filter buildFilter(String type, String key, String value) {
        switch (type) {
            case StringBeginsWith.FILTER_TYPE_NAME:
                return new StringBeginsWith(key, value);
            case StringContains.FILTER_TYPE_NAME:
                return new StringContains(key, value);
            case StringEquals.FILTER_TYPE_NAME:
                return new StringEquals(key, value);
            default:
                throw new IllegalArgumentException("Type " + type + " is not valid");
        }
    }
}
