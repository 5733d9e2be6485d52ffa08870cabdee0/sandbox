package com.redhat.service.smartevents.infra.exceptions.definitions.platform;

public class InvalidURLException extends InternalPlatformException {

    public InvalidURLException(String message) {
        super(message);
    }

    public InvalidURLException(String message, Throwable cause) {
        super(message, cause);
    }
}
