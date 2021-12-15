package com.redhat.service.bridge.infra.exceptions.definitions;

import javax.ws.rs.core.Response;

public class EventBridgeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EventBridgeException(String message) {
        super(message);
    }

    public EventBridgeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * The status code to be returned to the client when this Exception is raised. Sub-classes should
     * over-ride this.
     *
     * @return - The HTTP Status code to return to the client.
     */
    public int getStatusCode() {
        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }
}
