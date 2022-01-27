package com.redhat.service.bridge.infra.exceptions.definitions.platform;

public class SecretsNotFoundException extends InternalPlatformException {

    public SecretsNotFoundException(String message) {
        super(message);
    }

    public SecretsNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}