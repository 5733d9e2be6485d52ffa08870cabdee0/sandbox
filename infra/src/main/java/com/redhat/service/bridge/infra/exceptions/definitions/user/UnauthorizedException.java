package com.redhat.service.bridge.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class UnauthorizedException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.UNAUTHORIZED.getStatusCode();
    }
}
