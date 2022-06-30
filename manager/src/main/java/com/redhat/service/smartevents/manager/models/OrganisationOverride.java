package com.redhat.service.smartevents.manager.models;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrganisationOverride {

    @JsonProperty("org_id")
    @NotNull(message = "Organisation Id can't be null or empty")
    private String orgId;

    @JsonProperty("instance_type")
    @NotNull(message = "Instance type can't be null or empty")
    private ServiceLimitInstanceType instanceType;

    @JsonProperty("processor_limit")
    private int processorLimit;

    @JsonProperty("bridge_duration")
    private String bridgeDuration;

    @JsonProperty("instance_quota")
    private int instanceQuota;

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public ServiceLimitInstanceType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(ServiceLimitInstanceType instanceType) {
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

    public int getInstanceQuota() {
        return instanceQuota;
    }

    public void setInstanceQuota(int instanceQuota) {
        this.instanceQuota = instanceQuota;
    };

}
