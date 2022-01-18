package com.redhat.service.bridge.infra.exceptions.definitions.platform;

public class ConnectorDeletionException extends InternalPlatformException {

    public ConnectorDeletionException(String message) {
        super(message);
    }

    public ConnectorDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
