package com.redhat.service.smartevents.infra.v1.api.models.filters;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StringBeginsWith)) {
            return false;
        }
        StringBeginsWith that = (StringBeginsWith) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
