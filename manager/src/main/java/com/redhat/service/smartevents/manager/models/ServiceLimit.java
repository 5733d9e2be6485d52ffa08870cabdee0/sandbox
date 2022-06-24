package com.redhat.service.smartevents.manager.models;

import java.util.List;

public class ServiceLimit {

    private List<ServiceLimitInstance> serviceLimitInstances;
    private ServiceLimitInstanceType defaultInstanceType;
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
