package com.redhat.service.smartevents.infra.models.filters;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StringBeginsWith extends BaseFilter {
    public static final String FILTER_TYPE_NAME = "StringBeginsWith";

    @JsonProperty("values")
    private List<String> values;

    public StringBeginsWith() {
        super(FILTER_TYPE_NAME);
    }

    public StringBeginsWith(String key, List<String> values) {
        super(FILTER_TYPE_NAME, key);
        this.values = values;
    }

    @Override
    public List<String> getValue() {
        return values;
    }

}
