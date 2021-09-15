package com.redhat.service.bridge.infra.k8s.crds;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.dto.BridgeDTO;
import com.redhat.service.bridge.infra.dto.BridgeStatus;

// TODO: move to shard or shard-api. It is in this infra module because k8s module needs it atm
public class BridgeCustomResource {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("status")
    private BridgeStatus status;

    public BridgeCustomResource() {
    }

    public static BridgeCustomResource fromDTO(BridgeDTO dto) {
        BridgeCustomResource customResource = new BridgeCustomResource();
        customResource.id = dto.getId();
        customResource.name = dto.getName();
        customResource.endpoint = dto.getEndpoint();
        customResource.customerId = dto.getCustomerId();
        customResource.status = dto.getStatus();

        return customResource;
    }

    public BridgeDTO toDTO() {
        BridgeDTO dto = new BridgeDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setCustomerId(customerId);
        dto.setEndpoint(endpoint);
        dto.setStatus(status);

        return dto;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BridgeStatus getStatus() {
        return status;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setStatus(BridgeStatus status) {
        this.status = status;
    }
}
