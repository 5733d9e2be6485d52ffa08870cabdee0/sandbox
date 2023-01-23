package com.redhat.service.smartevents.manager.core.ams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganisationQuota {

    @JsonProperty("bridges_quota")
    private long bridgesQuota;

    @JsonProperty("processors_quota")
    private long processorsQuota;

    @JsonProperty("source_connectors_quota")
    private long sourceConnectorsQuota;

    @JsonProperty("sink_connectors_quota")
    private long sinkConnectorsQuota;

    public OrganisationQuota() {
    }

    public OrganisationQuota(long bridgesQuota, long processorsQuota, long sourceConnectorsQuota, long sinkConnectorsQuota) {
        this.bridgesQuota = bridgesQuota;
        this.processorsQuota = processorsQuota;
        this.sourceConnectorsQuota = sourceConnectorsQuota;
        this.sinkConnectorsQuota = sinkConnectorsQuota;
    }

    public long getBridgesQuota() {
        return bridgesQuota;
    }

    public long getProcessorsQuota() {
        return processorsQuota;
    }

    public long getSourceConnectorsQuota() {
        return sourceConnectorsQuota;
    }

    public long getSinkConnectorsQuota() {
        return sinkConnectorsQuota;
    }
}
