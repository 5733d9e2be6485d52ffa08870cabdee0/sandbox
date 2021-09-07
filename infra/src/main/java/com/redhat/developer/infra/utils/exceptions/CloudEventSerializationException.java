package com.redhat.developer.infra.utils.exceptions;

public class CloudEventSerializationException extends RuntimeException {

    public CloudEventSerializationException(String message) {
        super(message);
    }

    public CloudEventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}