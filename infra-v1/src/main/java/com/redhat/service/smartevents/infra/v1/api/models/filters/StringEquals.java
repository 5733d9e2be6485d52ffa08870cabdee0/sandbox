package com.redhat.service.smartevents.infra.v1.api.models.filters;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StringEquals extends BaseFilter {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StringEquals)) {
            return false;
        }
        StringEquals that = (StringEquals) o;
        return Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}
