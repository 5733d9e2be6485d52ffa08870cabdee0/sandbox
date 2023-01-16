package com.redhat.service.smartevents.infra.v1.api.models.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessorDTO extends BaseV1DTO {

    @JsonProperty("type")
    private ProcessorType type;

    @JsonProperty("definition")
    private ProcessorDefinition definition;

    @JsonProperty("bridgeId")
    private String bridgeId;

    @JsonProperty("kafkaConnection")
    private KafkaConnectionDTO kafkaConnection;

    public ProcessorDTO() {
    }

    public ProcessorDTO(String id, String name, String customerId, String owner, ManagedResourceStatusV1 status, ProcessorType type,
            ProcessorDefinition definition, String bridgeId, KafkaConnectionDTO kafkaConnection) {
        super(id, name, customerId, owner, status);
        this.type = type;
        this.definition = definition;
        this.bridgeId = bridgeId;
        this.kafkaConnection = kafkaConnection;
    }

    public ProcessorType getType() {
        return type;
    }

    public void setType(ProcessorType type) {
        this.type = type;
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
                "type=" + type +
                ", id='" + id + '\'' +
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
