package com.redhat.service.bridge.logging.api;

/**
 * An entry in a Log.
 * Implicit properties, 'date-stamp' and 'level' are resolved when the {@link LogEntry} is logged.
 * See {@link Logger#debug(LogEntry)}, {@link Logger#info(LogEntry)} and {@link Logger#error(LogEntry)}
 */
public interface LogEntry {

    Class<?> forClass();

    String getMessage();

}
