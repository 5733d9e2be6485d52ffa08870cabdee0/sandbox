package com.redhat.service.smartevents.infra.core.exceptions.definitions.platform;

public class VaultException extends InternalPlatformException {

    public VaultException(String message) {
        super(message);
    }

    public VaultException(String message, Throwable cause) {
        super(message, cause);
    }
}
