package com.redhat.service.bridge.manager.exceptions;

public class ConnectorCreationException extends EventBridgeManagerException {

    public ConnectorCreationException(String message) {
        super(message);
    }

    public ConnectorCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
