package com.redhat.service.smartevents.rhoas.exceptions;

import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;

public class RhoasClientException extends InternalPlatformException {

    public RhoasClientException(String message, Throwable failure) {
        super(message, failure);
    }

}
