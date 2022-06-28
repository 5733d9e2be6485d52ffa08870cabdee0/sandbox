package com.redhat.service.smartevents.infra.exceptions.definitions.user;

/**
 * The user has selected an invalid region as part of their request to create a bridge.
 */
public class InvalidRegionException extends ExternalUserException {

    public InvalidRegionException(String message) {
        super(message);
    }
}
