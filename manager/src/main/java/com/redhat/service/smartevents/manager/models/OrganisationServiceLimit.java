package com.redhat.service.smartevents.manager.models;

public class OrganisationServiceLimit {

    ServiceLimitInstanceType instanceType;
    private int processorLimit;
    private String bridgeDuration;
    private long instanceQuota;

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

    public long getInstanceQuota() {
        return instanceQuota;
    }

    public void setInstanceQuota(long instanceQuota) {
        this.instanceQuota = instanceQuota;
    }
}
