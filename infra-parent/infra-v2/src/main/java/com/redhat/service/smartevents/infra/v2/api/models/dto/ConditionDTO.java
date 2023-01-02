package com.redhat.service.smartevents.infra.v2.api.models.dto;

import java.time.ZonedDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionDTO {

    @JsonProperty("type")
    private String type;

    @JsonProperty("status")
    private ConditionStatus status;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("message")
    private String message;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("last_transition_time")
    private ZonedDateTime lastTransitionTime;

    public ConditionDTO() {
    }

    public ConditionDTO(String type, ConditionStatus status, ZonedDateTime lastTransitionTime) {
        this.type = type;
        this.status = status;
        this.lastTransitionTime = lastTransitionTime;
    }

    public ConditionDTO(String type, ConditionStatus status, String reason, String message, String errorCode) {
        this.type = type;
        this.status = status;
        this.reason = reason;
        this.message = message;
        this.errorCode = errorCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ConditionStatus getStatus() {
        return status;
    }

    public void setStatus(ConditionStatus status) {
        this.status = status;
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

    public ZonedDateTime getLastTransitionTime() {
        return lastTransitionTime;
    }

    public void setLastTransitionTime(ZonedDateTime lastTransitionTime) {
        this.lastTransitionTime = lastTransitionTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConditionDTO)) {
            return false;
        }
        ConditionDTO condition = (ConditionDTO) o;
        return Objects.equals(getType(), condition.getType())
                && getStatus() == condition.getStatus()
                && Objects.equals(getReason(), condition.getReason())
                && Objects.equals(getMessage(), condition.getMessage())
                && Objects.equals(getErrorCode(), condition.getErrorCode())
                && Objects.equals(getLastTransitionTime(), condition.getLastTransitionTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getStatus(), getReason(), getMessage(), getErrorCode(), getLastTransitionTime());
    }
}
