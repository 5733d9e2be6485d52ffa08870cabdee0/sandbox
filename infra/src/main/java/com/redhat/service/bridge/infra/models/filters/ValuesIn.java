package com.redhat.service.bridge.infra.models.filters;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ValuesIn extends BaseFilter<List<Object>> {
    public static final String FILTER_TYPE_NAME = "ValuesIn";

    @JsonProperty("values")
    private List<Object> values;

    public ValuesIn() {
        super(FILTER_TYPE_NAME);
    }

    @SuppressWarnings("unchecked")
    public ValuesIn(String key, String values) {
        super(FILTER_TYPE_NAME, key);
        try {
            this.values = ObjectMapperFactory.get().readValue(values, List.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The value is not a list");
        }
    }

    @Override
    public List<Object> getValue() {
        return values;
    }

}
