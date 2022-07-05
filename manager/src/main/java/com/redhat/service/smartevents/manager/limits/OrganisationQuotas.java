package com.redhat.service.smartevents.manager.limits;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrganisationQuotas {

    @JsonProperty("org_id")
    @NotNull(message = "Organisation Id can't be null or empty")
    private String orgId;

    @JsonProperty("instance_quotas")
    @NotNull(message = "Instance quota can't be null or empty")
    private List<InstanceQuota> instanceQuotas;

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public List<InstanceQuota> getInstanceQuotas() {
        return instanceQuotas;
    }

    public void setInstanceQuotas(List<InstanceQuota> instanceQuotas) {
        this.instanceQuotas = instanceQuotas;
    }
}
