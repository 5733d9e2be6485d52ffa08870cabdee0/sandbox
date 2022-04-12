package com.redhat.service.smartevents.rhoas.exceptions;

import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;

public class AppServicesException extends InternalPlatformException {

    private final int statusCode;

    public AppServicesException(String message, com.openshift.cloud.api.kas.auth.invoker.ApiException e) {
        super(String.format("%s (status=%d)", message, e.getCode()), e);
        statusCode = e.getCode();
    }

    public AppServicesException(String message, com.openshift.cloud.api.kas.invoker.ApiException e) {
        super(String.format("%s (status=%d)", message, e.getCode()), e);
        statusCode = e.getCode();
    }

    public int getStatusCode() {
        return statusCode;
    }
}
