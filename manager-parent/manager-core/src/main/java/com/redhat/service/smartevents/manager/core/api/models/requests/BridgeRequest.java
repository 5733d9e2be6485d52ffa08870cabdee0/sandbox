package com.redhat.service.smartevents.manager.core.api.models.requests;

import java.util.Objects;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BridgeRequest {

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

    public BridgeRequest() {
    }

    public BridgeRequest(String name, String cloudProvider, String region) {
        this.name = name;
        this.cloudProvider = cloudProvider;
        this.region = region;
    }

    public String getName() {
        return Objects.nonNull(name) ? name.trim() : null;
    }
}
