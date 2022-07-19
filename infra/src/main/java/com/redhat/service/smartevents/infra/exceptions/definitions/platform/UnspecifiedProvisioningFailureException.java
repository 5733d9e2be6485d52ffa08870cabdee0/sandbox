package com.redhat.service.smartevents.infra.exceptions.definitions.platform;

public class UnspecifiedProvisioningFailureException extends InternalPlatformException {

    public UnspecifiedProvisioningFailureException(String message) {
        super(message);
    }

    public UnspecifiedProvisioningFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
