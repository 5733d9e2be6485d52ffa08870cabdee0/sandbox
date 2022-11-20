package com.redhat.service.smartevents.infra.models.dto;

import java.util.Objects;

public class ConditionDTO {

    private String type;
    private String status;
    private String lastTransitionTime;
    private String reason;
    private String message;
    private String errorCode;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastTransitionTime() {
        return lastTransitionTime;
    }

    public void setLastTransitionTime(String lastTransitionTime) {
        this.lastTransitionTime = lastTransitionTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionDTO that = (ConditionDTO) o;
        return Objects.equals(type, that.type) && Objects.equals(status, that.status) && Objects.equals(lastTransitionTime, that.lastTransitionTime) && Objects.equals(reason, that.reason) && Objects.equals(message, that.message) && Objects.equals(errorCode, that.errorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, status, lastTransitionTime, reason, message, errorCode);
    }
}
