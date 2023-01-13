package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class NoQuotaAvailable extends BaseExternalUserException {

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
