package com.redhat.service.smartevents.infra.models.filters;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NumberIn extends BaseFilter {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NumberIn)) {
            return false;
        }
        NumberIn numbersIn = (NumberIn) o;
        return Objects.equals(values, numbersIn.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
