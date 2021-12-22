package com.redhat.service.bridge.rhoas.exceptions;

public class AppServicesException extends RuntimeException {

    public AppServicesException(String message, com.openshift.cloud.api.kas.auth.invoker.ApiException e) {
        super(String.format("%s (status=%d)", message, e.getCode()), e);
    }

    public AppServicesException(String message, com.openshift.cloud.api.kas.invoker.ApiException e) {
        super(String.format("%s (status=%d)", message, e.getCode()), e);
    }

}
