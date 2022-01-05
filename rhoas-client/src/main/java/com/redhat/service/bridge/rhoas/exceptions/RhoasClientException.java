package com.redhat.service.bridge.rhoas.exceptions;

import java.util.List;

import com.redhat.service.bridge.infra.exceptions.definitions.platform.InternalPlatformException;

import io.smallrye.mutiny.CompositeException;

public class RhoasClientException extends InternalPlatformException {

    public RhoasClientException(String message, List<Throwable> failures) {
        super(message, failures.size() > 1 ? new CompositeException(failures) : failures.get(0));
    }

}
