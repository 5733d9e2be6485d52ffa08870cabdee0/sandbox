package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform;

public class ProvisioningTimeOutException extends BaseInternalPlatformException {

    public static final String TIMEOUT_FAILURE_MESSAGE = "The timeout to process Work for Resource of type '%s' with Id '%s' was exceeded.";

    public ProvisioningTimeOutException(String message) {
        super(message);
    }

}
