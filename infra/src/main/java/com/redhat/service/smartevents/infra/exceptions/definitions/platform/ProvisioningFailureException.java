package com.redhat.service.smartevents.infra.exceptions.definitions.platform;

import com.redhat.service.smartevents.infra.exceptions.ProvidesReason;

public class ProvisioningFailureException extends InternalPlatformException implements ProvidesReason {

    public ProvisioningFailureException(String message) {
        super(message);
    }

    public ProvisioningFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
