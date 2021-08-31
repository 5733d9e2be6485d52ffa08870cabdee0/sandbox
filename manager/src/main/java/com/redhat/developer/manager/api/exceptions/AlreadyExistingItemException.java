package com.redhat.developer.manager.api.exceptions;

import javax.ws.rs.core.Response;

public class AlreadyExistingItemException extends EventConnectManagerException {

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