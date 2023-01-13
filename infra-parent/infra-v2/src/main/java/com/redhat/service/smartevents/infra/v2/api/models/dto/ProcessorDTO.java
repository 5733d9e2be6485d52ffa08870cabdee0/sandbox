package com.redhat.service.smartevents.infra.v2.api.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessorDTO extends BaseResourceDTO {

    @JsonProperty("flows")
    private ObjectNode flows;

    @JsonProperty("bridgeId")
    private String bridgeId;

    public ProcessorDTO() {
    }

    public ProcessorDTO(String id,
            String name,
            ObjectNode flows,
            String bridgeId,
            String customerId,
            String owner,
            OperationType operationType,
            int timeout) {
        super(id, name, customerId, owner, operationType, timeout);
        this.flows = flows;
        this.bridgeId = bridgeId;
    }

    public ObjectNode getFlows() {
        return flows;
    }

    public void setFlows(ObjectNode flows) {
        this.flows = flows;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }

}
