package com.redhat.service.smartevents.manager.v2.api.user.models.requests;

import java.util.Objects;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.v2.api.user.validators.ValidCloudProviderV2;

@ValidCloudProviderV2
public class BridgeRequestV2 {

    @NotEmpty(message = "Bridge name cannot be null or empty")
    @JsonProperty("name")
    protected String name;

    @NotEmpty(message = "Cloud Provider cannot be null or empty.")
    @JsonProperty("cloud_provider")
    protected String cloudProvider;

    @NotEmpty(message = "Region cannot be null or empty.")
    @JsonProperty("region")
    protected String region;

    public String getCloudProvider() {
        return cloudProvider;
    }

    public String getRegion() {
        return region;
    }

    public BridgeRequestV2() {
    }

    public BridgeRequestV2(String name, String cloudProvider, String region) {
        this.name = name;
        this.cloudProvider = cloudProvider;
        this.region = region;
    }

    public String getName() {
        return Objects.nonNull(name) ? name.trim() : null;
    }
}
