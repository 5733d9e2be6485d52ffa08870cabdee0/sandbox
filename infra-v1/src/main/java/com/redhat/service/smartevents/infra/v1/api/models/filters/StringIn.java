package com.redhat.service.smartevents.infra.v1.api.models.filters;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StringIn extends BaseFilter {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StringIn)) {
            return false;
        }
        StringIn that = (StringIn) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
