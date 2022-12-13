package com.redhat.service.smartevents.infra.v1.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.api.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessorManagedResourceStatusUpdateDTO extends ManagedResourceStatusUpdateDTO {

    @JsonProperty("bridgeId")
    private String bridgeId;

    public ProcessorManagedResourceStatusUpdateDTO() {
    }

    public ProcessorManagedResourceStatusUpdateDTO(String id,
            String customerId,
            String bridgeId,
            ManagedResourceStatus status) {
        super(id, customerId, status);
        this.bridgeId = bridgeId;
    }

    public ProcessorManagedResourceStatusUpdateDTO(String id,
            String customerId,
            String bridgeId,
            ManagedResourceStatus status,
            BridgeErrorInstance bridgeErrorInstance) {
        super(id, customerId, status, bridgeErrorInstance);
        this.bridgeId = bridgeId;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }
}

