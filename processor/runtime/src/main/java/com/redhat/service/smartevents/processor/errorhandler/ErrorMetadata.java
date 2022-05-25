package com.redhat.service.smartevents.processor.errorhandler;

public class ErrorMetadata {

    private String bridgeId;
    private String processorId;
    private String originalEventId;
    private ErrorType type;

    public enum ErrorType {
        ERROR,
        WARNING,
        INFORMATION
    }

    public ErrorMetadata(String bridgeId, String processorId, String originalEventId, ErrorType type) {
        this.bridgeId = bridgeId;
        this.processorId = processorId;
        this.originalEventId = originalEventId;
        this.type = type;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public String getProcessorId() {
        return processorId;
    }

    public String getOriginalEventId() {
        return originalEventId;
    }

    public ErrorType getType() {
        return type;
    }
}
