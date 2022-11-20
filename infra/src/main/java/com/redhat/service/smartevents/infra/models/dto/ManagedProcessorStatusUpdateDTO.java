package com.redhat.service.smartevents.infra.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ManagedProcessorStatusUpdateDTO {

    @JsonProperty("processorId")
    private String processorId;

    @JsonProperty("conditions")
    private List<ConditionDTO> conditions;

    public ManagedProcessorStatusUpdateDTO() {
    }

    public ManagedProcessorStatusUpdateDTO(String processorId, List<ConditionDTO> conditions) {
        this.processorId = processorId;
        this.conditions = conditions;
    }

    public String getProcessorId() {
        return processorId;
    }

    public void setProcessorId(String processorId) {
        this.processorId = processorId;
    }

    public List<ConditionDTO> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionDTO> conditions) {
        this.conditions = conditions;
    }
}
