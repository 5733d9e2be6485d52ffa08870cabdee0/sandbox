package com.redhat.service.smartevents.infra.models.filters;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValuesIn extends BaseFilter {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ValuesIn)) {
            return false;
        }
        ValuesIn valuesIn = (ValuesIn) o;
        return Objects.equals(values, valuesIn.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
