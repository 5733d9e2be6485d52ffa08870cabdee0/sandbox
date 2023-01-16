package com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform;

public class DeserializationException extends BaseInternalPlatformException {

    public DeserializationException(String message) {
        super(message);
    }

    public DeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
