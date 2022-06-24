package com.redhat.service.smartevents.manager.models;

public class OrganisationServiceLimit {

    private int processorLimit;
    private long bridgeDuration;
    private long instanceQuota;

    public int getProcessorLimit() {
        return processorLimit;
    }

    public void setProcessorLimit(int processorLimit) {
        this.processorLimit = processorLimit;
    }

    public long getBridgeDuration() {
        return bridgeDuration;
    }

    public void setBridgeDuration(long bridgeDuration) {
        this.bridgeDuration = bridgeDuration;
    }

    public long getInstanceQuota() {
        return instanceQuota;
    }

    public void setInstanceQuota(long instanceQuota) {
        this.instanceQuota = instanceQuota;
    }
}
