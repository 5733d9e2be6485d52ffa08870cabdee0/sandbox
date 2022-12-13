package com.redhat.service.smartevents.manager.core.ams;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrganisationQuota {

    @JsonProperty("bridges_quota")
    private long bridgesQuota;

    @JsonProperty("processors_quota")
    private long processorsQuota;

    public OrganisationQuota() {
    }

    public OrganisationQuota(long bridgesQuota, long processorsQuota) {
        this.bridgesQuota = bridgesQuota;
        this.processorsQuota = processorsQuota;
    }

    public long getBridgesQuota() {
        return bridgesQuota;
    }

    public void setBridgesQuota(long bridgesQuota) {
        this.bridgesQuota = bridgesQuota;
    }

    public long getProcessorsQuota() {
        return processorsQuota;
    }

    public void setProcessorsQuota(long processorsQuota) {
        this.processorsQuota = processorsQuota;
    }
}

