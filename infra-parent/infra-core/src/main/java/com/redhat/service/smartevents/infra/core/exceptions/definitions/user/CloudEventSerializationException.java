package com.redhat.service.smartevents.infra.core.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class CloudEventSerializationException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public CloudEventSerializationException(String message) {
        super(message);
    }

    public CloudEventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}