package com.redhat.service.smartevents.infra.core.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ManagedResourceStatusUpdateDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("status")
    private ManagedResourceStatus status;

    @JsonProperty("bridgeErrorInstance")
    private BridgeErrorInstance bridgeErrorInstance;

    public ManagedResourceStatusUpdateDTO() {
    }

    public ManagedResourceStatusUpdateDTO(String id,
            String customerId,
            ManagedResourceStatus status) {
        this(id, customerId, status, null);
    }

    public ManagedResourceStatusUpdateDTO(String id,
            String customerId,
            ManagedResourceStatus status,
            BridgeErrorInstance bridgeErrorInstance) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.bridgeErrorInstance = bridgeErrorInstance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public ManagedResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ManagedResourceStatus status) {
        this.status = status;
    }

    public BridgeErrorInstance getBridgeErrorInstance() {
        return bridgeErrorInstance;
    }

    public void setBridgeErrorInstance(BridgeErrorInstance bridgeErrorInstance) {
        this.bridgeErrorInstance = bridgeErrorInstance;
    }

}
