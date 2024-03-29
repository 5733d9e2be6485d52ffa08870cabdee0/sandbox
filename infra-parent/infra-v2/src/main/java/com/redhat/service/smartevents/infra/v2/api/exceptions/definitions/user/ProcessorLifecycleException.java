package com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class ProcessorLifecycleException extends BaseExternalUserException {

    private static final long serialVersionUID = 1L;

    public ProcessorLifecycleException(String message) {
        super(message);
    }

    public ProcessorLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
