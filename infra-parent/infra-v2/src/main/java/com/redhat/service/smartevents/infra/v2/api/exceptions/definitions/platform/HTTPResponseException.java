package com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform;

public class HTTPResponseException extends BaseInternalPlatformException {

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
