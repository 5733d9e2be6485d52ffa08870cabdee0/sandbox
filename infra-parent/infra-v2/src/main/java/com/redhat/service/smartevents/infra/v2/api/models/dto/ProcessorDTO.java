package com.redhat.service.smartevents.infra.v2.api.models.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessorDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("flows")
    private ObjectNode flows;

    @JsonProperty("bridgeId")
    private String bridgeId;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("operation")
    private OperationType operationType;

    @JsonProperty("generation")
    private long generation;

    public ProcessorDTO() {
    }

    public ProcessorDTO(String id, String name, ObjectNode flows, String bridgeId, String customerId, String owner, OperationType operationType) {
        this.id = id;
        this.name = name;
        this.flows = flows;
        this.bridgeId = bridgeId;
        this.customerId = customerId;
        this.owner = owner;
        this.operationType = operationType;
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

    public ObjectNode getFlows() {
        return flows;
    }

    public void setFlows(ObjectNode flows) {
        this.flows = flows;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProcessorDTO that = (ProcessorDTO) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
