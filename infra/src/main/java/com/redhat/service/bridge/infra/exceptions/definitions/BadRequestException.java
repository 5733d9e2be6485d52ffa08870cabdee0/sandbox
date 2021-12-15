package com.redhat.service.bridge.infra.exceptions.definitions;

import javax.ws.rs.core.Response;

public class BadRequestException extends EventBridgeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * The status code to be returned to the client when this Exception is raised. Sub-classes should
     * over-ride this.
     *
     * @return - The HTTP Status code to return to the client.
     */
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
