package com.redhat.service.smartevents.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class ServiceLimitException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public ServiceLimitException(String message) {
        super(message);
    }

    public ServiceLimitException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.FORBIDDEN.getStatusCode();
    }
}
