package com.redhat.service.bridge.rhoas;

import java.util.List;

import io.smallrye.mutiny.CompositeException;

public class RhoasServiceException extends RuntimeException {

    public RhoasServiceException(String message, List<Throwable> failures) {
        super(message, failures.size() > 1 ? new CompositeException(failures) : failures.get(0));
    }

}
