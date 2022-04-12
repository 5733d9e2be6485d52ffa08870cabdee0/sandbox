package com.redhat.service.smartevents.infra.models.filters;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValuesIn extends BaseFilter<List<Object>> {
    public static final String FILTER_TYPE_NAME = "ValuesIn";

    @JsonProperty("values")
    private List<Object> values;

    public ValuesIn() {
        super(FILTER_TYPE_NAME);
    }

    public ValuesIn(String key, List<Object> values) {
        super(FILTER_TYPE_NAME, key);
        this.values = values;
    }

    @Override
    public List<Object> getValue() {
        return values;
    }

}
