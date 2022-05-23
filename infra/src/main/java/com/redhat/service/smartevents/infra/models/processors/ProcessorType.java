package com.redhat.service.smartevents.infra.models.processors;

import java.util.Arrays;
import java.util.Objects;

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

    @SuppressWarnings("unused")
    // Required for JAX-RS deserialisation. See @javax.ws.rs.QueryParam.
    public static ProcessorType fromString(String type) {
        return Arrays
                .stream(ProcessorType.values())
                .filter(t -> Objects.equals(t.value, type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("ProcessorType '%s' unknown.", type)));
    }

    ProcessorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
