package com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform;

public class InvalidURLException extends BaseInternalPlatformException {

    public InvalidURLException(String message) {
        super(message);
    }

    public InvalidURLException(String message, Throwable cause) {
        super(message, cause);
    }
}
