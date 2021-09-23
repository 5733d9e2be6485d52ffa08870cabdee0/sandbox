package com.redhat.service.bridge.actions;

public class ActionProviderException extends RuntimeException {

    public ActionProviderException(String message) {
        super(message);
    }

    public ActionProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
