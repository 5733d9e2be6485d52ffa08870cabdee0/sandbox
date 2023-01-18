package com.redhat.service.smartevents.infra.v2.api.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceConfigurationDTO {

    @JsonProperty("kafkaConnection")
    private KafkaConnectionDTO kafkaConnection;

    public SourceConfigurationDTO() {

    }

    public SourceConfigurationDTO(KafkaConnectionDTO kafkaConnection) {
        this.kafkaConnection = kafkaConnection;
    }

    public KafkaConnectionDTO getKafkaConnection() {
        return kafkaConnection;
    }

    public void setKafkaConnection(KafkaConnectionDTO kafkaConnection) {
        this.kafkaConnection = kafkaConnection;
    }
}
