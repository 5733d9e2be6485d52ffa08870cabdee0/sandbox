package com.redhat.service.bridge.infra.models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StringEquals extends Filter {
    public static final String FILTER_TYPE_NAME = "StringEquals";

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
        super(key, value);
    }

    @Override
    public String getValueAsString() {
        return value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValueFromString(String value) {
        this.value = value;
    }
}
