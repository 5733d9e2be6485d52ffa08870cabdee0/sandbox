package com.redhat.service.smartevents.infra.exceptions.definitions.user;

/**
 * The user has selected an invalid Cloud Provider as part of their request to create a Bridge.
 */
public class InvalidCloudProviderException extends ExternalUserException {

    public InvalidCloudProviderException(String message) {
        super(message);
    }
}
