package com.redhat.service.bridge.logging.impl;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.logging.api.LogEntry;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEntryImpl implements LogEntry {

    @JsonIgnore
    private Class<?> clazz;

    @JsonProperty("message")
    private String message;

    public LogEntryImpl(Class<?> clazz,
            String message) {
        this.clazz = Objects.requireNonNull(clazz);
        this.message = Objects.requireNonNull(message);
    }

    @Override
    public Class<?> forClass() {
        return clazz;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
