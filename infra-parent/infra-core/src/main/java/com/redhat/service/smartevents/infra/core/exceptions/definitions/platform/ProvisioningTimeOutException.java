package com.redhat.service.smartevents.infra.core.exceptions.definitions.platform;

public class ProvisioningTimeOutException extends InternalPlatformException {

    public static final String TIMEOUT_FAILURE_MESSAGE = "The timeout to process Work for Resource of type '%s' with Id '%s' was exceeded.";

    public ProvisioningTimeOutException(String message) {
        super(message);
    }

}
