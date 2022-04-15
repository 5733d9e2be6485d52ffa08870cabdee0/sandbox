package com.redhat.service.smartevents.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

/**
 * This class represents all the exceptions that are caused by the user interaction. For example the user asks for a resource
 * that does not exist, the user sends a wrong configuration etc.
 *
 * All the subclasses have to be included in the /resources/exceptionInfo.json because they are directly visible on the catalog.
 */
public class ExternalUserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExternalUserException(String message) {
        super(message);
    }

    public ExternalUserException(String message, Throwable cause) {
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
