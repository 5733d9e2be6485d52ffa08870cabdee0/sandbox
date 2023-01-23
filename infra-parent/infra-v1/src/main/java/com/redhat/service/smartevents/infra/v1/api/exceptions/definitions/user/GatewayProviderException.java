package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class GatewayProviderException extends BaseExternalUserException {

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
