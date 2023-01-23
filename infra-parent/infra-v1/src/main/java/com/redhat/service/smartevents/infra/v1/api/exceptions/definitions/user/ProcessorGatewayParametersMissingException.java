package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class ProcessorGatewayParametersMissingException extends BaseExternalUserException {

    private static final long serialVersionUID = 1L;

    public ProcessorGatewayParametersMissingException(String message) {
        super(message);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
