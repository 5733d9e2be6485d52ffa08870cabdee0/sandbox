package com.redhat.service.smartevents.performance.webhook.exceptions;

public class BridgeNotFoundException extends Exception {

    private static final long serialVersionUID = 2319415509210342979L;

    private final String bridgeId;

    public BridgeNotFoundException(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public String getMessage() {
        return String.format("Bridge not found with id %s", bridgeId);
    }
}

