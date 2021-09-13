package com.redhat.service.bridge.infra.utils.exceptions;

public class CloudEventDeserializationException extends RuntimeException {

    public CloudEventDeserializationException(String message) {
        super(message);
    }

    public CloudEventDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}