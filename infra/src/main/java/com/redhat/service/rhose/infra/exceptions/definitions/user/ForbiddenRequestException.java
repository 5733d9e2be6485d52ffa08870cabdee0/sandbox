package com.redhat.service.rhose.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class ForbiddenRequestException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public ForbiddenRequestException(String message) {
        super(message);
    }

    public ForbiddenRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.FORBIDDEN.getStatusCode();
    }
}