package com.redhat.service.smartevents.infra.models.processors;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProcessorType {
    SOURCE("source"),
    SINK("sink");

    final String value;

    // We can not annotate the property `value` directly with `@JsonValue`. See https://issues.redhat.com/browse/MGDOBR-595
    @JsonValue
    public String serialize() {
        return value;
    }

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
