package com.redhat.service.smartevents.infra.core.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class ProcessorFilterDefinitionException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public ProcessorFilterDefinitionException(String message) {
        super(message);
    }

    public ProcessorFilterDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
