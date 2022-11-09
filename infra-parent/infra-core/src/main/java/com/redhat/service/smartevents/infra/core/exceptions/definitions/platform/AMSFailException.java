package com.redhat.service.smartevents.infra.core.exceptions.definitions.platform;

public class AMSFailException extends InternalPlatformException {

    public AMSFailException(String message) {
        super(message);
    }

    public AMSFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
