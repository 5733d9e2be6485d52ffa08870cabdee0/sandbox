package com.redhat.developer.infra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessorDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("bridge")
    private BridgeDTO bridge;

    @JsonProperty("status")
    private BridgeStatus status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BridgeDTO getBridge() {
        return bridge;
    }

    public void setBridge(BridgeDTO bridge) {
        this.bridge = bridge;
    }

    public BridgeStatus getStatus() {
        return status;
    }

    public void setStatus(BridgeStatus status) {
        this.status = status;
    }
}
