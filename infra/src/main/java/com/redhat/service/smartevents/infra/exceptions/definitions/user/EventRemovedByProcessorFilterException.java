package com.redhat.service.smartevents.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class EventRemovedByProcessorFilterException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public EventRemovedByProcessorFilterException(String message) {
        super(message);
    }

    public EventRemovedByProcessorFilterException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }

}
