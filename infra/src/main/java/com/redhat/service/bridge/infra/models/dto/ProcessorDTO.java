package com.redhat.service.bridge.infra.models.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;

public class ProcessorDTO {

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

    @JsonProperty("status")
    private BridgeStatus status;

    @JsonProperty("kafkaConnection")
    private KafkaConnectionDTO kafkaConnection;

    public ProcessorDTO() {
    }

    public ProcessorDTO(String id, String name, ProcessorDefinition definition, String bridgeId, String customerId, BridgeStatus status, KafkaConnectionDTO kafkaConnection) {
        this.id = id;
        this.name = name;
        this.bridgeId = bridgeId;
        this.customerId = customerId;
        this.status = status;
        this.definition = definition;
        this.kafkaConnection = kafkaConnection;
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

    public BridgeStatus getStatus() {
        return status;
    }

    public void setStatus(BridgeStatus status) {
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
}
