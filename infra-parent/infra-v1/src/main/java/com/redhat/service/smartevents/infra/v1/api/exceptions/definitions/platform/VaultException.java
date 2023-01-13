package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform;

public class VaultException extends BaseInternalPlatformException {

    public VaultException(String message) {
        super(message);
    }

    public VaultException(String message, Throwable cause) {
        super(message, cause);
    }
}
