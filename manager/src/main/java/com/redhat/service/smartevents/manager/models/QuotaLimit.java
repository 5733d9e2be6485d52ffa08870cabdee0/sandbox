package com.redhat.service.smartevents.manager.models;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QuotaLimit {

    @JsonProperty("quota_type")
    private QuotaType quotaType;

    @JsonProperty("processor_limit")
    private int processorLimit;

    @JsonProperty("bridge_duration")
    private Duration bridgeDuration;

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public int getProcessorLimit() {
        return processorLimit;
    }

    public void setProcessorLimit(int processorLimit) {
        this.processorLimit = processorLimit;
    }

    public Duration getBridgeDuration() {
        return bridgeDuration;
    }

    public void setBridgeDuration(Duration bridgeDuration) {
        this.bridgeDuration = bridgeDuration;
    }
}
