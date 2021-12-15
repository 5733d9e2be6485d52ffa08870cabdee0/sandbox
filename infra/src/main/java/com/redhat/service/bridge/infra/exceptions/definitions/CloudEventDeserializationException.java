package com.redhat.service.bridge.infra.exceptions.definitions;

import javax.ws.rs.core.Response;

public class CloudEventDeserializationException extends EventBridgeException {

    private static final long serialVersionUID = 1L;

    public CloudEventDeserializationException(String message) {
        super(message);
    }

    public CloudEventDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}