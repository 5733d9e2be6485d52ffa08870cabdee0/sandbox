package com.redhat.service.bridge.infra.models.filters;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StringIn extends BaseFilter<List<String>> {
    public static final String FILTER_TYPE_NAME = "StringIn";

    @JsonProperty("values")
    private List<String> values;

    public StringIn() {
        super(FILTER_TYPE_NAME);
    }

    public StringIn(String key, List<String> values) {
        super(FILTER_TYPE_NAME, key);
        this.values = values;
    }

    @Override
    public List<String> getValue() {
        return values;
    }
}
