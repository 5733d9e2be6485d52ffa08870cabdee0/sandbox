package com.redhat.service.smartevents.manager.models;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceLimit {

    @JsonProperty("service_limit_instances")
    @NotEmpty(message = "Service Limit instance types can't be null or empty")
    private List<ServiceLimitInstance> serviceLimitInstances;

    @JsonProperty("default_instance_type")
    @NotNull(message = "Default Instance Type can't be null or empty")
    private ServiceLimitInstanceType defaultInstanceType;

    @JsonProperty("organisation_overrides")
    @Valid
    private List<OrganisationOverride> organisationOverrides;

    public List<ServiceLimitInstance> getInstanceTypes() {
        return serviceLimitInstances;
    }

    public void setInstanceTypes(List<ServiceLimitInstance> serviceLimitInstances) {
        this.serviceLimitInstances = serviceLimitInstances;
    }

    public ServiceLimitInstanceType getDefaultInstanceType() {
        return defaultInstanceType;
    }

    public void setDefaultInstanceType(ServiceLimitInstanceType defaultInstanceType) {
        this.defaultInstanceType = defaultInstanceType;
    }

    public List<OrganisationOverride> getOrganisationOverrides() {
        return organisationOverrides;
    }

    public void setOrganisationOverrides(List<OrganisationOverride> organisationOverrides) {
        this.organisationOverrides = organisationOverrides;
    }
}
