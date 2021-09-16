package com.redhat.service.bridge.infra.filters;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StringBeginsWith extends Filter {
    public static final String FILTER_TYPE_NAME = "StringBeginsWith";

    @JsonProperty("type")
    private String type = FILTER_TYPE_NAME;

    @JsonProperty("values")
    private List<String> values;

    public StringBeginsWith() {
    }

    public StringBeginsWith(String key, String values) {
        this.key = key;
        setValueFromString(values);
    }

    public String getType() {
        return type;
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
