package com.redhat.service.smartevents.manager.limits;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.models.QuotaLimit;

public class ServiceLimitConfig {

    @JsonProperty("quota_limits")
    @NotEmpty(message = "Quota limits can't be null or empty")
    @Valid
    private List<QuotaLimit> quotaLimits;

    @JsonProperty("default_quotas")
    @NotNull(message = "Default quotas can't be null or empty")
    @Valid
    private List<InstanceQuota> defaultQuotas;

    @JsonProperty("organisation_quotas")
    @Valid
    private List<OrganisationQuotas> organisationQuotas;

    public ServiceLimitConfig() {
        organisationQuotas = Collections.emptyList();
    }

    public List<QuotaLimit> getQuotaLimits() {
        return quotaLimits;
    }

    public void setQuotaLimits(List<QuotaLimit> quotaLimits) {
        this.quotaLimits = quotaLimits;
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
