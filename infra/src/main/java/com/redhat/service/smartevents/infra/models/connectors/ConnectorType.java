package com.redhat.service.smartevents.infra.models.connectors;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConnectorType {
    SOURCE("source"),
    SINK("sink");

    @JsonValue
    final String value;

    ConnectorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
