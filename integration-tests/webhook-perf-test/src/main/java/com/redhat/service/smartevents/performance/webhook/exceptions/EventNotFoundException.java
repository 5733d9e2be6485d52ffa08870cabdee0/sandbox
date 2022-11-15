package com.redhat.service.smartevents.performance.webhook.exceptions;

public class EventNotFoundException extends Exception {

    private static final long serialVersionUID = 2319415509210342979L;

    private final Long id;

    public EventNotFoundException(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return String.format("Event not found with id %s", id);
    }
}