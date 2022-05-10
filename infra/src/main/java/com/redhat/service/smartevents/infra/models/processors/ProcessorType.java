package com.redhat.service.smartevents.infra.models.processors;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProcessorType {
    SOURCE("source"),
    SINK("sink");

    @JsonValue
    final String value;

    ProcessorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProcessorType fromValue(String value) {
        for (ProcessorType p : values()) {
            if (p.getValue().equals(value)) {
                return p;
            }
        }
        throw new IllegalArgumentException(String.format("No ProcessorType with value \"%s\"", value));
    }
}
