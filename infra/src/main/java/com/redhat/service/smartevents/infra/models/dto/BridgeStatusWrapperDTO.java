package com.redhat.service.smartevents.infra.models.dto;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.exceptions.BridgeError;

public class BridgeStatusWrapperDTO {

    @NotNull
    @JsonProperty("bridge")
    private BridgeDTO bridge;

    @JsonProperty("exception")
    private BridgeError exception;

    public BridgeStatusWrapperDTO() {
    }

    public BridgeStatusWrapperDTO(BridgeDTO bridge) {
        this.bridge = bridge;
    }

    public BridgeStatusWrapperDTO(BridgeDTO bridge, BridgeError exception) {
        this.bridge = bridge;
        this.exception = exception;
    }

    public BridgeDTO getBridge() {
        return bridge;
    }

    public BridgeError getException() {
        return exception;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BridgeStatusWrapperDTO)) {
            return false;
        }
        BridgeStatusWrapperDTO that = (BridgeStatusWrapperDTO) o;
        return getBridge().equals(that.getBridge()) && Objects.equals(getException(), that.getException());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBridge(), getException());
    }

    @Override
    public String toString() {
        return "BridgeStatusWrapperDTO{" +
                "bridge=" + bridge +
                ", exception=" + exception +
                '}';
    }
}
