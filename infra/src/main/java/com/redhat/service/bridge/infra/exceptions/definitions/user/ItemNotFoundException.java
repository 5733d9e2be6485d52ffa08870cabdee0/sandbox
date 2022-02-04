package com.redhat.service.bridge.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class ItemNotFoundException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public ItemNotFoundException(String message) {
        super(message);
    }

    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.NOT_FOUND.getStatusCode();
    }
}