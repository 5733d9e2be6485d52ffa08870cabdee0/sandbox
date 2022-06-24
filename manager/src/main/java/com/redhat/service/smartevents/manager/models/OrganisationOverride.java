package com.redhat.service.smartevents.manager.models;

public class OrganisationOverride {

    private String orgId;
    private ServiceLimitInstanceType orgInstanceType;
    private int processorLimit;
    private long bridgeDuration;
    private int instanceQuota;

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public ServiceLimitInstanceType getOrgInstanceType() {
        return orgInstanceType;
    }

    public void setOrgInstanceType(ServiceLimitInstanceType orgInstanceType) {
        this.orgInstanceType = orgInstanceType;
    }

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

    public int getInstanceQuota() {
        return instanceQuota;
    }

    public void setInstanceQuota(int instanceQuota) {
        this.instanceQuota = instanceQuota;
    };

}
