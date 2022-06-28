package com.redhat.service.smartevents.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class ProcessorMultipleGatewayException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public ProcessorMultipleGatewayException(String message) {
        super(message);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
