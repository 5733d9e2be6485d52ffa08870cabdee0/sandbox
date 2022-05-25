package com.redhat.service.smartevents.processor.errorhandler;

public class Error {

    private ErrorMetadata metadata;
    private String payload;
    private String message;

    public Error(ErrorMetadata metadata, String payload, String message) {
        this.metadata = metadata;
        this.payload = payload;
        this.message = message;
    }

    public ErrorMetadata getMetadata() {
        return metadata;
    }

    public String getPayload() {
        return payload;
    }

    public String getMessage() {
        return message;
    }
}
