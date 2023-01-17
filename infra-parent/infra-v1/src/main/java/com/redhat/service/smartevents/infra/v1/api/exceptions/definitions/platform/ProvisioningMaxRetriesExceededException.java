package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform;

public class ProvisioningMaxRetriesExceededException extends BaseInternalPlatformException {

    public static final String RETRIES_FAILURE_MESSAGE = "The maximum number of re-tries for Resource of type '%s' with Id '%s' was exceeded.";

    public ProvisioningMaxRetriesExceededException(String message) {
        super(message);
    }

}
