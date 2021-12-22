package com.redhat.service.bridge.rhoas.exceptions;

import java.util.List;

import io.smallrye.mutiny.CompositeException;

public class RhoasClientException extends RuntimeException {

    public RhoasClientException(String message, List<Throwable> failures) {
        super(message, failures.size() > 1 ? new CompositeException(failures) : failures.get(0));
    }

}
