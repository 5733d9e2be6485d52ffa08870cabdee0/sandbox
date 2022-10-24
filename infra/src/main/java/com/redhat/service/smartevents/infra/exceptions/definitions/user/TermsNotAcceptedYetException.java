package com.redhat.service.smartevents.infra.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class TermsNotAcceptedYetException extends ExternalUserException {

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