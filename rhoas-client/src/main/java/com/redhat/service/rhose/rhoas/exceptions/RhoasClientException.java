package com.redhat.service.rhose.rhoas.exceptions;

import com.redhat.service.rhose.infra.exceptions.definitions.platform.InternalPlatformException;

public class RhoasClientException extends InternalPlatformException {

    public RhoasClientException(String message, Throwable failure) {
        super(message, failure);
    }

}
