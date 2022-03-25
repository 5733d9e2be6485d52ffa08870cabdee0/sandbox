package com.redhat.service.bridge.logging.impl.slf4j;

import java.time.ZonedDateTime;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.service.bridge.logging.api.LogEntry;
import com.redhat.service.bridge.logging.api.Logger;

@ApplicationScoped
public class LoggerImpl implements Logger {

    private final ObjectMapper mapper = new ObjectMapper();

    public LoggerImpl() {
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void debug(LogEntry entry) {
        org.slf4j.Logger logger = LoggerFactory.getLogger(entry.forClass());
        if (logger.isDebugEnabled()) {
            logger.debug(toLogEntry(Level.DEBUG, entry));
        }
    }

    @Override
    public void info(LogEntry entry) {
        org.slf4j.Logger logger = LoggerFactory.getLogger(entry.forClass());
        if (logger.isInfoEnabled()) {
            logger.info(toLogEntry(Level.INFO, entry));
        }
    }

    @Override
    public void warn(LogEntry entry) {
        org.slf4j.Logger logger = LoggerFactory.getLogger(entry.forClass());
        if (logger.isWarnEnabled()) {
            logger.warn(toLogEntry(Level.WARN, entry));
        }
    }

    @Override
    public void error(LogEntry entry) {
        org.slf4j.Logger logger = LoggerFactory.getLogger(entry.forClass());
        if (logger.isErrorEnabled()) {
            logger.error(toLogEntry(Level.ERROR, entry));
        }
    }

    protected String toLogEntry(final Level level, final LogEntry entry) {
        final ZonedDateTime now = ZonedDateTime.now();
        LogEntryInternal internal = new LogEntryInternal() {
            @Override
            public ZonedDateTime getTimestamp() {
                return now;
            }

            @Override
            public Level getLevel() {
                return level;
            }

            @Override
            public Class<?> forClass() {
                return entry.forClass();
            }

            @Override
            public String getMessage() {
                return entry.getMessage();
            }
        };
        try {
            return mapper.writeValueAsString(internal);
        } catch (JsonProcessingException e) {
            return entry.getMessage();
        }
    }

}
