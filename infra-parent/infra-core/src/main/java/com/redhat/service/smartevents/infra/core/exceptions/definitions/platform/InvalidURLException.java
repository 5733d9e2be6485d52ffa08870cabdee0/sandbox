package com.redhat.service.smartevents.infra.core.exceptions.definitions.platform;

public class InvalidURLException extends InternalPlatformException {

    public InvalidURLException(String message) {
        super(message);
    }

    public InvalidURLException(String message, Throwable cause) {
        super(message, cause);
    }
}
