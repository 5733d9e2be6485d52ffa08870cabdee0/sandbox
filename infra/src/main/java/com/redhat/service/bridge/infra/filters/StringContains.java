package com.redhat.service.bridge.infra.filters;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StringContains extends Filter {
    public static final String FILTER_TYPE_NAME = "StringContains";

    @JsonProperty("type")
    private String type = FILTER_TYPE_NAME;

    public String getType() {
        return type;
    }

    @JsonProperty("values")
    private List<String> values;

    public StringContains() {
    }

    public StringContains(String key, String value) {
        this.key = key;
        setValueFromString(value);
    }

    @Override
    public String getStringValue() {
        return values.toString();
    }

    @Override
    public void setValueFromString(String value) {
        try {
            this.values = new ObjectMapper().readValue(value, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
