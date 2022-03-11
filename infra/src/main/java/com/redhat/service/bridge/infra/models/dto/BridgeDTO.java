package com.redhat.service.bridge.infra.models.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BridgeDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("status")
    private ManagedResourceStatus status;

    @JsonProperty("kafkaConnection")
    private KafkaConnectionDTO kafkaConnection;

    public BridgeDTO() {
    }

    public BridgeDTO(String id, String name, String endpoint, String customerId, ManagedResourceStatus status, KafkaConnectionDTO kafkaConnection) {
        this.id = id;
        this.name = name;
        this.endpoint = endpoint;
        this.customerId = customerId;
        this.status = status;
        this.kafkaConnection = kafkaConnection;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setStatus(ManagedResourceStatus status) {
        this.status = status;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setKafkaConnection(KafkaConnectionDTO kafkaConnection) {
        this.kafkaConnection = kafkaConnection;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getName() {
        return name;
    }

    public ManagedResourceStatus getStatus() {
        return status;
    }

    public KafkaConnectionDTO getKafkaConnection() {
        return kafkaConnection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BridgeDTO bridgeDTO = (BridgeDTO) o;
        return id.equals(bridgeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BridgeDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", customerId='" + customerId + '\'' +
                ", status=" + status + '\'' +
                ", kafkaConnection=" + kafkaConnection.toString() +
                '}';
    }
}
