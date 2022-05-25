package com.redhat.service.smartevents.processor.errorhandler;

public interface ErrorPublisher {

    void sendError(ErrorMetadata metadata, String payload, Exception exception);

    void sendError(ErrorMetadata metadata, String payload, String message);

}
