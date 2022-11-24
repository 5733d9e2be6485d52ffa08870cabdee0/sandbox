package com.redhat.service.smartevents.infra.v2.api.models.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.Operation;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeDTO {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("tlsCertificate")
    private String tlsCertificate;

    @JsonProperty("tlsKey")
    private String tlsKey;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("kafkaConnection")
    private KafkaConnectionDTO kafkaConnection;

    @JsonProperty("operation")
    private Operation operation;

    public BridgeDTO() {
    }

    public BridgeDTO(String id, String name, String endpoint, String tlsCertificate, String tlsKey, String customerId, String owner, KafkaConnectionDTO kafkaConnection, Operation operation) {
        this.id = id;
        this.name = name;
        this.endpoint = endpoint;
        this.tlsCertificate = tlsCertificate;
        this.tlsKey = tlsKey;
        this.customerId = customerId;
        this.owner = owner;
        this.kafkaConnection = kafkaConnection;
        this.operation = operation;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setTlsCertificate(String tlsCertificate) {
        this.tlsCertificate = tlsCertificate;
    }

    public void setTlsKey(String tlsKey) {
        this.tlsKey = tlsKey;
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

    public String getOwner() {
        return owner;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getTlsCertificate() {
        return tlsCertificate;
    }

    public String getTlsKey() {
        return tlsKey;
    }

    public String getName() {
        return name;
    }

    public KafkaConnectionDTO getKafkaConnection() {
        return kafkaConnection;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
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

}
