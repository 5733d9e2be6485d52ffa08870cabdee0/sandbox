package com.redhat.service.smartevents.manager.core.api.models.requests;

import java.util.Objects;

import javax.validation.constraints.NotEmpty;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AbstractBridgeRequest {

    @NotEmpty(message = "Bridge name cannot be null or empty")
    @JsonProperty("name")
    @Schema(
            description = "The name of the bridge",
            example = "bridge1")
    protected String name;

    @NotEmpty(message = "Cloud Provider cannot be null or empty.")
    @Schema(
            description = "The cloud provider where the bridge resides",
            example = "aws")
    @JsonProperty("cloud_provider")
    protected String cloudProvider;

    @NotEmpty(message = "Region cannot be null or empty.")
    @JsonProperty("region")
    @Schema(
            description = "The cloud provider region where the bridge resides",
            example = "us-east")
    protected String region;

    public String getCloudProvider() {
        return cloudProvider;
    }

    public String getRegion() {
        return region;
    }

    public AbstractBridgeRequest() {
    }

    public AbstractBridgeRequest(String name, String cloudProvider, String region) {
        this.name = name;
        this.cloudProvider = cloudProvider;
        this.region = region;
    }

    public String getName() {
        return Objects.nonNull(name) ? name.trim() : null;
    }
}
