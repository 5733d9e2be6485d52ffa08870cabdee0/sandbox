package com.redhat.service.bridge.infra.exceptions.definitions.platform;

public class ConnectorNotFoundException extends InternalPlatformException {

    public ConnectorNotFoundException(String message) {
        super(message);
    }

    public ConnectorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
