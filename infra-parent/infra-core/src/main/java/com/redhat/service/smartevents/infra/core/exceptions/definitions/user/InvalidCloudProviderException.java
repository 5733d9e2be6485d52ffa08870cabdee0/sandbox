package com.redhat.service.smartevents.infra.core.exceptions.definitions.user;

import javax.ws.rs.core.Response;

/**
 * The user has selected an invalid Cloud Provider as part of their request to create a Bridge.
 */
public class InvalidCloudProviderException extends ExternalUserException {

    public InvalidCloudProviderException(String message) {
        super(message);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
