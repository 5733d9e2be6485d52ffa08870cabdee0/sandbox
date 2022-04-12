package com.redhat.service.rhose.infra.exceptions.definitions.platform;

public class HTTPResponseException extends InternalPlatformException {

    int statusCode;

    public HTTPResponseException(String message) {
        super(message);
    }

    public HTTPResponseException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HTTPResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getStatusCode() {
        return statusCode;
    }
}
