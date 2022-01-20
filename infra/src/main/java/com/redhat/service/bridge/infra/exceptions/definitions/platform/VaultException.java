package com.redhat.service.bridge.infra.exceptions.definitions.platform;

public class VaultException extends InternalPlatformException {

    public VaultException(String message) {
        super(message);
    }

    public VaultException(String message, Throwable cause) {
        super(message, cause);
    }
}
