package com.redhat.service.bridge.infra.exceptions.definitions.platform;

public class HTTPResponseException extends InternalPlatformException {

    public HTTPResponseException(String message) {
        super(message);
    }

    public HTTPResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
