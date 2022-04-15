package com.redhat.service.smartevents.infra.models.processors;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProcessorType {
    SOURCE("source"),
    SINK("sink");

    @JsonValue
    final String jsonValue;

    ProcessorType(String jsonValue) {
        this.jsonValue = jsonValue;
    }
}