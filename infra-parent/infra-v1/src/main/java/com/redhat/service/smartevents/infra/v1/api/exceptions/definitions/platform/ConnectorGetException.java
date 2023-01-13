package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform;

public class ConnectorGetException extends BaseInternalPlatformException {

    public ConnectorGetException(String message) {
        super(message);
    }

    public ConnectorGetException(String message, Throwable cause) {
        super(message, cause);
    }
}
