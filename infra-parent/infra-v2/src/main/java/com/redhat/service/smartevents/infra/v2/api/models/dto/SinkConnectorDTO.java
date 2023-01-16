package com.redhat.service.smartevents.infra.v2.api.models.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SinkConnectorDTO extends BaseV2DTO {

    @JsonProperty("bridgeId")
    private String bridgeId;

    @JsonProperty("kafkaConnection")
    private KafkaConnectionDTO kafkaConnection;

    public SinkConnectorDTO() {
    }

    public SinkConnectorDTO(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public SinkConnectorDTO(String id, String name, String customerId, String owner, OperationType operationType, int timeoutSeconds, String bridgeId) {
        super(id, name, customerId, owner, operationType, timeoutSeconds);
        this.bridgeId = bridgeId;
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
        if (!super.equals(o)) {
            return false;
        }
        SinkConnectorDTO that = (SinkConnectorDTO) o;
        return Objects.equals(bridgeId, that.bridgeId) && Objects.equals(kafkaConnection, that.kafkaConnection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bridgeId, kafkaConnection);
    }
}
