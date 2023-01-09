package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform;

public class AMSFailException extends BaseInternalPlatformException {

    public AMSFailException(String message) {
        super(message);
    }

    public AMSFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
