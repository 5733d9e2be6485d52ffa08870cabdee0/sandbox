package com.redhat.service.smartevents.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class NoQuotaAvailable extends ExternalUserException {

    private static final long serialVersionUID = 1L;

    public NoQuotaAvailable(String message) {
        super(message);
    }

    public NoQuotaAvailable(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.PAYMENT_REQUIRED.getStatusCode();
    }
}