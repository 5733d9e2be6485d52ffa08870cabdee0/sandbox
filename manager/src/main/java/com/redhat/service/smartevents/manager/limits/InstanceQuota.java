package com.redhat.service.smartevents.manager.limits;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.models.QuotaType;

public class InstanceQuota {
    @JsonProperty("quota_type")
    @NotNull(message = "Quota Type can't be null or empty")
    private QuotaType instanceType;

    @JsonProperty("quota")
    @Min(value = 1L, message = "Quota can't be empty")
    private int quota;

    public QuotaType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(QuotaType instanceType) {
        this.instanceType = instanceType;
    }

    public int getQuota() {
        return quota;
    }

    public void setQuota(int quota) {
        this.quota = quota;
    }
}
