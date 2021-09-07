package com.redhat.developer.manager.exceptions;

import javax.ws.rs.core.Response;

public class ItemNotFoundException extends EventBridgeManagerException {

    public ItemNotFoundException(String message) {
        super(message);
    }

    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}