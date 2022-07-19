package com.redhat.service.smartevents.infra.exceptions.definitions.platform;

public class ProvisioningFailureException extends InternalPlatformException {

    ProvisioningFailureException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return "ProvisioningFailureException{" +
                "message='" + getMessage() + "'" +
                '}';
    }
}
