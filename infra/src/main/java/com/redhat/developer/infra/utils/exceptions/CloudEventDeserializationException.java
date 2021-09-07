package com.redhat.developer.infra.utils.exceptions;

public class CloudEventDeserializationException extends RuntimeException {

    public CloudEventDeserializationException(String message) {
        super(message);
    }

    public CloudEventDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}