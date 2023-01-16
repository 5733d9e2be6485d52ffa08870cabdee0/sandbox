package com.redhat.service.smartevents.infra.v1.api.models.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeDTO extends BaseV1DTO {

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("tlsCertificate")
    private String tlsCertificate;

    @JsonProperty("tlsKey")
    private String tlsKey;

    @JsonProperty("kafkaConnection")
    private KafkaConnectionDTO kafkaConnection;

    public BridgeDTO() {
    }

    public BridgeDTO(String id,
            String name,
            String endpoint,
            String tlsCertificate,
            String tlsKey,
            String customerId,
            String owner,
            ManagedResourceStatusV1 status,
            KafkaConnectionDTO kafkaConnection) {
        super(id, name, customerId, owner, status);
        this.endpoint = endpoint;
        this.tlsCertificate = tlsCertificate;
        this.tlsKey = tlsKey;
        this.kafkaConnection = kafkaConnection;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getTlsCertificate() {
        return tlsCertificate;
    }

    public void setTlsCertificate(String tlsCertificate) {
        this.tlsCertificate = tlsCertificate;
    }

    public String getTlsKey() {
        return tlsKey;
    }

    public void setTlsKey(String tlsKey) {
        this.tlsKey = tlsKey;
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
                ", tlsCertificate='REDACTED'" +
                ", tlsKey='REDACTED'" +
                ", endpoint='" + endpoint + '\'' +
                ", customerId='" + customerId + '\'' +
                ", owner='" + owner + '\'' +
                ", status=" + status +
                ", kafkaConnection=" + kafkaConnection +
                '}';
    }
}
