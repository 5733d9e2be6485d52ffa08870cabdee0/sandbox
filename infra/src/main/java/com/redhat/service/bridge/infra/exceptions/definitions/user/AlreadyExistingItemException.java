package com.redhat.service.bridge.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class AlreadyExistingItemException extends UserFaultException {

    private static final long serialVersionUID = 1L;

    public AlreadyExistingItemException(String message) {
        super(message);
    }

    public AlreadyExistingItemException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}