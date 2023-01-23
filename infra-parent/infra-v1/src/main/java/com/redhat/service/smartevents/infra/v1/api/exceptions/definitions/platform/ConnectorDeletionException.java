package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform;

public class ConnectorDeletionException extends BaseInternalPlatformException {

    public ConnectorDeletionException(String message) {
        super(message);
    }

    public ConnectorDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
