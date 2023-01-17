package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform;

public class ConnectorUpdateException extends BaseInternalPlatformException {

    public ConnectorUpdateException(String message) {
        super(message);
    }

    public ConnectorUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
