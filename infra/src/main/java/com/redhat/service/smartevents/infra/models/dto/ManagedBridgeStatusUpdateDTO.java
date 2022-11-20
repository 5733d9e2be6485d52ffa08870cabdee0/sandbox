package com.redhat.service.smartevents.infra.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ManagedBridgeStatusUpdateDTO {

    @JsonProperty("bridgeId")
    private String bridgeId;

    @JsonProperty("conditions")
    private List<ConditionDTO> conditions;

    public ManagedBridgeStatusUpdateDTO() {
    }

    public ManagedBridgeStatusUpdateDTO(String bridgeId, List<ConditionDTO> conditions) {
        this.bridgeId = bridgeId;
        this.conditions = conditions;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public List<ConditionDTO> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionDTO> conditions) {
        this.conditions = conditions;
    }
}
