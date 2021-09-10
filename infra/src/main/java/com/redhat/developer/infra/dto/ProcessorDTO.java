package com.redhat.developer.infra.dto;

import java.util.Objects;

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

    public ProcessorDTO() {
    }

    public ProcessorDTO(String id, String name, BridgeDTO bridge, BridgeStatus status) {
        this.id = id;
        this.name = name;
        this.bridge = bridge;
        this.status = status;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProcessorDTO that = (ProcessorDTO) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
