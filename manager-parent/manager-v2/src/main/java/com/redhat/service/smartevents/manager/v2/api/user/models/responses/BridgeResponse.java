package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.core.api.models.responses.BaseManagedResourceResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BridgeResponse extends BaseManagedResourceResponse {

    @JsonProperty("name")
    @NotNull
    @Schema(
            description = "The name of the bridge",
            example = "bridge1")
    protected String name;

    @JsonProperty("endpoint")
    @Schema(
            description = "The HTTPS endpoint on which the bridge accepts events",
            example = "https://example.com/bridge")
    private String endpoint;

    @JsonProperty("cloud_provider")
    @Schema(
            description = "The cloud provider where the bridge resides",
            example = "aws")
    private String cloudProvider;

    @JsonProperty("region")
    @Schema(
            description = "The cloud provider region where the bridge resides",
            example = "us-east")
    private String region;

    @JsonProperty("status_message")
    @Schema(
            description = "A detailed status message in case there is a problem with the bridge")
    private String statusMessage;

    public BridgeResponse() {
        super("Bridge");
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

}
