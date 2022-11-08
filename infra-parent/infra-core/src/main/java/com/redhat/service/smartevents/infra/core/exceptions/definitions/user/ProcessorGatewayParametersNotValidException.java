package com.redhat.service.smartevents.infra.core.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class ProcessorGatewayParametersNotValidException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public ProcessorGatewayParametersNotValidException(String message) {
        super(message);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
