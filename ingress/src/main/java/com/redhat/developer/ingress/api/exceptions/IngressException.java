package com.redhat.developer.ingress.api.exceptions;

import javax.ws.rs.core.Response;

public class IngressException extends RuntimeException {

    public IngressException(String message) {
        super(message);
    }

    public IngressException(String message, Throwable cause) {
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
