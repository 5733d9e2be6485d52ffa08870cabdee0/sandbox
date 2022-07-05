package com.redhat.service.smartevents.manager.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InstanceLimit {

    @JsonProperty("instance_type")
    private LimitInstanceType instanceType;

    @JsonProperty("processor_limit")
    private int processorLimit;

    @JsonProperty("bridge_duration")
    private String bridgeDuration;

    public LimitInstanceType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(LimitInstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public int getProcessorLimit() {
        return processorLimit;
    }

    public void setProcessorLimit(int processorLimit) {
        this.processorLimit = processorLimit;
    }

    public String getBridgeDuration() {
        return bridgeDuration;
    }

    public void setBridgeDuration(String bridgeDuration) {
        this.bridgeDuration = bridgeDuration;
    }
}
