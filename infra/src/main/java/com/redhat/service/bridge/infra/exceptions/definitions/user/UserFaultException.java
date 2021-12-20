package com.redhat.service.bridge.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

/**
 * This class represents all the exceptions that are caused by the user mistake. For example the user asks for a resource
 * that does not exist, the user sends a wrong configuration etc..
 *
 * All the subclasses have to be included in the /resources/exceptionInfo.json because they are directly visible on the catalog.
 */
public class UserFaultException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UserFaultException(String message) {
        super(message);
    }

    public UserFaultException(String message, Throwable cause) {
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
