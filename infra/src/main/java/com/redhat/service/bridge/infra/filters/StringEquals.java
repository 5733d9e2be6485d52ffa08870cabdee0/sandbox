package com.redhat.service.bridge.infra.filters;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StringEquals extends Filter {
    public static final String FILTER_TYPE_NAME = "StringEndsWith";

    @JsonProperty("type")
    private String type = FILTER_TYPE_NAME;

    public String getType() {
        return type;
    }

    @JsonProperty("value")
    private String value;

    public StringEquals() {
    }

    public StringEquals(String key, String value) {
        this.key = key;
        setValueFromString(value);
    }

    @Override
    public String getStringValue() {
        return value;
    }

    @Override
    public void setValueFromString(String value) {
        this.value = value;
    }
}
