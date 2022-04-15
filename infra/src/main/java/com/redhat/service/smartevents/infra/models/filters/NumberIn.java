package com.redhat.service.smartevents.infra.models.filters;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NumberIn extends BaseFilter<List<Double>> {
    public static final String FILTER_TYPE_NAME = "NumberIn";

    @JsonProperty("values")
    private List<Double> values;

    public NumberIn() {
        super(FILTER_TYPE_NAME);
    }

    public NumberIn(String key, List<Double> values) {
        super(FILTER_TYPE_NAME, key);
        this.values = values;
    }

    @Override
    public List<Double> getValue() {
        return values;
    }

}
