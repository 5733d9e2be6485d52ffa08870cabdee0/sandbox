package com.redhat.service.smartevents.infra.exceptions.definitions.platform;

public class ConnectorCreationException extends InternalPlatformException {

    public ConnectorCreationException(String message) {
        super(message);
    }

    public ConnectorCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
