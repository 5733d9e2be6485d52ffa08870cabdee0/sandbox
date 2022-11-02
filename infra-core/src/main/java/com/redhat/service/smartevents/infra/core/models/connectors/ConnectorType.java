package com.redhat.service.smartevents.infra.core.models.connectors;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConnectorType {
    SOURCE("source"),
    SINK("sink");

    final String value;

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
