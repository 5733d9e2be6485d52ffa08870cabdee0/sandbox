package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class TermsNotAcceptedYetException extends BaseExternalUserException {

    private static final long serialVersionUID = 1L;

    public TermsNotAcceptedYetException(String message) {
        super(message);
    }

    public TermsNotAcceptedYetException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
