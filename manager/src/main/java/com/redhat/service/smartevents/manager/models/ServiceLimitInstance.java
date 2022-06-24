package com.redhat.service.smartevents.manager.models;

public class ServiceLimitInstance {

    private ServiceLimitInstanceType instanceType;
    private int processorLimit;
    private long bridgeDuration;
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
