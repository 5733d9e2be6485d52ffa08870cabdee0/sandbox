package com.redhat.service.bridge.rhoas.exceptions;

import com.redhat.service.bridge.infra.exceptions.definitions.platform.InternalPlatformException;

public class RhoasClientException extends InternalPlatformException {

    public RhoasClientException(String message, Throwable failure) {
        super(message, failure);
    }

}
