package com.redhat.service.smartevents.infra.models.connectors;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConnectorType {
    SOURCE("source"),
    SINK("sink");

    final String value;

    // We can not annotate the property `value` directly with `@JsonValue`. See https://issues.redhat.com/browse/MGDOBR-595
    @JsonValue
    public String serialize() {
        return value;
    }

    ConnectorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
