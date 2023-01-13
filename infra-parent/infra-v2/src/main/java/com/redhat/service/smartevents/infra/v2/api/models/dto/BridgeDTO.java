package com.redhat.service.smartevents.infra.v2.api.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeDTO extends BaseResourceDTO {

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
            KafkaConnectionDTO kafkaConnection,
            OperationType operationType,
            int timeout) {
        super(id, name, customerId, owner, operationType, timeout);
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
}
