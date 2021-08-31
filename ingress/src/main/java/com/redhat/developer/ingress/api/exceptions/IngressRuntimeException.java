package com.redhat.developer.ingress.api.exceptions;

import javax.ws.rs.core.Response;

public class IngressRuntimeException extends IngressException {

    public IngressRuntimeException(String message) {
        super(message);
    }

    public IngressRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}