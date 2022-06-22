package com.redhat.service.smartevents.infra.models.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

public class ProcessorDTO {

    @JsonProperty("type")
    private ProcessorType type;

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("definition")
    private ProcessorDefinition definition;

    @JsonProperty("bridgeId")
    private String bridgeId;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("status")
    private ManagedResourceStatus status;

    @JsonProperty("kafkaConnection")
    private KafkaConnectionDTO kafkaConnection;

    public ProcessorType getType() {
        return type;
    }

    public void setType(ProcessorType type) {
        this.type = type;
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

    public ProcessorDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ProcessorDefinition definition) {
        this.definition = definition;
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

    public ManagedResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ManagedResourceStatus status) {
        this.status = status;
    }

    public KafkaConnectionDTO getKafkaConnection() {
        return kafkaConnection;
    }

    public void setKafkaConnection(KafkaConnectionDTO kafkaConnection) {
        this.kafkaConnection = kafkaConnection;
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

    @Override
    public String toString() {
        return "ProcessorDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", definition=" + definition +
                ", bridgeId='" + bridgeId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", owner='" + owner + '\'' +
                ", status=" + status +
                ", kafkaConnection=" + kafkaConnection +
                '}';
    }
}
