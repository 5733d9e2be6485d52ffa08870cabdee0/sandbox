package com.redhat.service.smartevents.infra.exceptions.definitions.platform;

public class ProvisioningMaxRetriesExceededException extends ProvisioningFailureException {

    public ProvisioningMaxRetriesExceededException(String message) {
        super(message);
    }

}
