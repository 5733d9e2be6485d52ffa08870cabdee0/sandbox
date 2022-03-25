package com.redhat.service.bridge.logging.impl.slf4j;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.logging.api.Logger;
import com.redhat.service.bridge.logging.impl.LogEntryImpl;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggerImplTest {

    private static final String MESSAGE_PREFIX = "Message for ";

    private static final LoggerImpl LOGGER = new LoggerImpl();

    @Test
    void testToLogEntryDebug() {
        testToLogEntry(Logger.Level.DEBUG);
    }

    @Test
    void testToLogEntryInfo() {
        testToLogEntry(Logger.Level.INFO);
    }

    @Test
    void testToLogEntryWarn() {
        testToLogEntry(Logger.Level.WARN);
    }

    @Test
    void testToLogEntryError() {
        testToLogEntry(Logger.Level.ERROR);
    }

    private void testToLogEntry(Logger.Level level) {
        String e = LOGGER.toLogEntry(level,
                new LogEntryImpl(LoggerImplTest.class, MESSAGE_PREFIX + level.name()));

        assertThat(e).isNotBlank();
        assertThat(e).contains("\"level\":\"" + level.name() + "\"");
        assertThat(e).contains("\"message\":\"" + MESSAGE_PREFIX + level.name() + "\"");
        assertThat(e).contains("\"timestamp\":");
        assertThat(e).doesNotContain("clazz");
    }
}
