package com.redhat.service.smartevents.manager.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceLimitInstance {

    @JsonProperty("instance_type")
    private ServiceLimitInstanceType instanceType;

    @JsonProperty("processor_limit")
    private int processorLimit;

    @JsonProperty("bridge_duration")
    private long bridgeDuration;

    @JsonProperty("instance_quota")
    private int instanceQuota;

    public void setInstanceType(ServiceLimitInstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public ServiceLimitInstanceType getInstanceType() {
        return instanceType;
    }

    public void setProcessorLimit(int processorLimit) {
        this.processorLimit = processorLimit;
    }

    public int getProcessorLimit() {
        return processorLimit;
    }

    public void setBridgeDuration(long bridgeDuration) {
        this.bridgeDuration = bridgeDuration;
    }

    public long getBridgeDuration() {
        return bridgeDuration;
    }

    public void setInstanceQuota(int instanceQuota) {
        this.instanceQuota = instanceQuota;
    }

    public int getInstanceQuota() {
        return instanceQuota;
    }
}
