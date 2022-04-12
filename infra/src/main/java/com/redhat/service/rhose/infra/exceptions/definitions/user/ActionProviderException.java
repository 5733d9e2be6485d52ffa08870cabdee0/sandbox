package com.redhat.service.rhose.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class ActionProviderException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public ActionProviderException(String message) {
        super(message);
    }

    public ActionProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
