package com.redhat.service.smartevents.manager.limits;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.models.InstanceLimit;

public class ServiceLimitConfig {

    @JsonProperty("instance_limits")
    @NotEmpty(message = "Instance limits types can't be null or empty")
    @Valid
    private List<InstanceLimit> instanceLimits;

    @JsonProperty("default_quotas")
    @NotNull(message = "Default quotas can't be null or empty")
    @Valid
    private List<InstanceQuota> defaultQuotas;

    @JsonProperty("organisation_quotas")
    @Valid
    private List<OrganisationQuotas> organisationQuotas;

    public List<InstanceLimit> getInstanceLimits() {
        return instanceLimits;
    }

    public void setInstanceLimits(List<InstanceLimit> instanceLimits) {
        this.instanceLimits = instanceLimits;
    }

    public List<InstanceQuota> getDefaultQuotas() {
        return defaultQuotas;
    }

    public void setDefaultQuotas(List<InstanceQuota> defaultQuotas) {
        this.defaultQuotas = defaultQuotas;
    }

    public List<OrganisationQuotas> getOrganisationQuotas() {
        return organisationQuotas;
    }

    public void setOrganisationQuotas(List<OrganisationQuotas> organisationQuotas) {
        this.organisationQuotas = organisationQuotas;
    }
}
