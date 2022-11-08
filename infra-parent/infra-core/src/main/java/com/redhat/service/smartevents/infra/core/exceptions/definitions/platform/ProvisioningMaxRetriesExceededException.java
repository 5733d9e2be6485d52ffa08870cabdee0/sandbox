package com.redhat.service.smartevents.infra.core.exceptions.definitions.platform;

public class ProvisioningMaxRetriesExceededException extends InternalPlatformException {

    public static final String RETRIES_FAILURE_MESSAGE = "The maximum number of re-tries for Resource of type '%s' with Id '%s' was exceeded.";

    public ProvisioningMaxRetriesExceededException(String message) {
        super(message);
    }

}
