package com.redhat.service.smartevents.manager.limits;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.models.LimitInstanceType;

public class InstanceQuota {
    @JsonProperty("instance_type")
    @NotNull(message = "Instance Type can't be null or empty")
    private LimitInstanceType instanceType;

    @JsonProperty("quota")
    @Min(value = 1L, message = "Quota can't be empty")
    private int quota;

    public LimitInstanceType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(LimitInstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public int getQuota() {
        return quota;
    }

    public void setQuota(int quota) {
        this.quota = quota;
    }
}
