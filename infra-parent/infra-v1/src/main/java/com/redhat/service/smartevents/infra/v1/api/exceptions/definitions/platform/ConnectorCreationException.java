package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform;

public class ConnectorCreationException extends BaseInternalPlatformException {

    public ConnectorCreationException(String message) {
        super(message);
    }

    public ConnectorCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
