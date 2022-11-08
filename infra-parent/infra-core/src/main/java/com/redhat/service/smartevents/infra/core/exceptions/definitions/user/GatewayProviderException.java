package com.redhat.service.smartevents.infra.core.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class GatewayProviderException extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public GatewayProviderException(String message) {
        super(message);
    }

    public GatewayProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
