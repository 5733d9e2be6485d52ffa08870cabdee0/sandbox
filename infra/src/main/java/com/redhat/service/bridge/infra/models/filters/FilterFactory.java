package com.redhat.service.bridge.infra.models.filters;

public class FilterFactory {
    public static BaseFilter buildFilter(String type, String key, String value) {
        switch (type) {
            case StringBeginsWith.FILTER_TYPE_NAME:
                return new StringBeginsWith(key, value);
            case StringContains.FILTER_TYPE_NAME:
                return new StringContains(key, value);
            case StringEquals.FILTER_TYPE_NAME:
                return new StringEquals(key, value);
            case ValuesIn.FILTER_TYPE_NAME:
                return new ValuesIn(key, value);
            default:
                throw new IllegalArgumentException("Type " + type + " is not valid");
        }
    }
}
