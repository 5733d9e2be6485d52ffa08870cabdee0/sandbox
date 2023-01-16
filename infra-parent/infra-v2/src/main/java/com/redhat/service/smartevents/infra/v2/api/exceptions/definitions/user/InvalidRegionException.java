package com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user;

import javax.ws.rs.core.Response;

/**
 * The user has selected an invalid region as part of their request to create a bridge.
 */
public class InvalidRegionException extends BaseExternalUserException {

    public InvalidRegionException(String message) {
        super(message);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
