package com.redhat.service.smartevents.infra.v2.api.models.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.models.dtos.BaseDTO;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseV2DTO extends BaseDTO {

    @JsonProperty("operation")
    private OperationType operationType;

    @JsonProperty("generation")
    private long generation;

    @JsonProperty("timeoutSeconds")
    private int timeoutSeconds;

    protected BaseV2DTO() {
    }

    protected BaseV2DTO(String id, String name, String customerId, String owner, OperationType operationType, int timeoutSeconds) {
        super(id, name, customerId, owner);
        this.operationType = operationType;
        this.timeoutSeconds = timeoutSeconds;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long generation) {
        this.generation = generation;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseV2DTO dto = (BaseV2DTO) o;
        return id.equals(dto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BaseV2DTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", customerId='" + customerId + '\'' +
                ", owner='" + owner + '\'' +
                ", operationType=" + operationType +
                ", generation=" + generation +
                ", timeoutSeconds=" + timeoutSeconds +
                '}';
    }
}
