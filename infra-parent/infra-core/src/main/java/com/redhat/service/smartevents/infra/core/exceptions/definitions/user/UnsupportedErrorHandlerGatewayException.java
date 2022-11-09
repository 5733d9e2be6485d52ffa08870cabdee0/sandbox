package com.redhat.service.smartevents.infra.core.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class UnsupportedErrorHandlerGatewayException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public UnsupportedErrorHandlerGatewayException(String message) {
        super(message);
    }

    public UnsupportedErrorHandlerGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
