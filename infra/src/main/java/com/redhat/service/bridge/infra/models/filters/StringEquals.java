package com.redhat.service.bridge.infra.models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StringEquals extends BaseFilter<String> {
    public static final String FILTER_TYPE_NAME = "StringEquals";

    @JsonProperty("value")
    private String value;

    public StringEquals() {
        super(FILTER_TYPE_NAME);
    }

    public StringEquals(String key, String value) {
        super(FILTER_TYPE_NAME, key);
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
