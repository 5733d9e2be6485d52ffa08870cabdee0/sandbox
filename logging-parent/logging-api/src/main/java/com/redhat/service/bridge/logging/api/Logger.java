package com.redhat.service.bridge.logging.api;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Writes the {@link LogEntry} to a log.
 */
public interface Logger {

    void debug(LogEntry entry);

    void info(LogEntry entry);

    void warn(LogEntry entry);

    void error(LogEntry entry);

    enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    interface LogEntryInternal extends LogEntry {

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
        @JsonProperty("timestamp")
        ZonedDateTime getTimestamp();

        @JsonProperty("level")
        Level getLevel();

    }

}
