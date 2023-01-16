package com.redhat.service.smartevents.infra.v2.api.models.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseResourceDTO {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("operation")
    private OperationType operationType;

    @JsonProperty("generation")
    private long generation;

    @JsonProperty("timeoutSeconds")
    private int timeoutSeconds;

    protected BaseResourceDTO() {
    }

    protected BaseResourceDTO(String id, String name, String customerId, String owner, OperationType operationType, int timeoutSeconds) {
        this.id = id;
        this.name = name;
        this.customerId = customerId;
        this.owner = owner;
        this.operationType = operationType;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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
        BaseResourceDTO dto = (BaseResourceDTO) o;
        return id.equals(dto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
