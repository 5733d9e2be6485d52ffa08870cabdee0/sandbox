package com.redhat.service.bridge.executor.actions;

public class ActionInvokerException extends RuntimeException {

    public ActionInvokerException(String message) {
        super(message);
    }

    public ActionInvokerException(String message, Throwable cause) {
        super(message, cause);
    }
}
