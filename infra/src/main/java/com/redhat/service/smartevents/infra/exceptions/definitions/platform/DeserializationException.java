package com.redhat.service.smartevents.infra.exceptions.definitions.platform;

public class DeserializationException extends InternalPlatformException {

    public DeserializationException(String message) {
        super(message);
    }

    public DeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
