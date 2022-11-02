package com.redhat.service.smartevents.manager.v1.api.models.requests;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.models.gateways.Action;
import com.redhat.service.smartevents.manager.v1.api.user.validators.processors.ValidCloudProvider;
import com.redhat.service.smartevents.manager.v1.api.user.validators.processors.ValidErrorHandler;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;

@ValidCloudProvider
@ValidErrorHandler
public class BridgeRequest {

    @NotEmpty(message = "Bridge name cannot be null or empty")
    @JsonProperty("name")
    protected String name;

    @JsonProperty("error_handler")
    @Valid
    protected Action errorHandler;

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

    public BridgeRequest(String name, String cloudProvider, String region, Action errorHandler) {
        this(name, cloudProvider, region);
        this.errorHandler = errorHandler;
    }

    public Bridge toEntity() {
        return new Bridge(getName());
    }

    public String getName() {
        return Objects.nonNull(name) ? name.trim() : null;
    }

    public Action getErrorHandler() {
        return errorHandler;
    }
}
