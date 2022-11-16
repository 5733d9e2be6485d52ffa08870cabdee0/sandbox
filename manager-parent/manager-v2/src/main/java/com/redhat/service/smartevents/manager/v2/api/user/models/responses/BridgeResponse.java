package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.core.api.models.responses.BaseManagedResourceResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BridgeResponse extends BaseManagedResourceResponse {

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("cloud_provider")
    private String cloudProvider;

    @JsonProperty("region")
    private String region;

    @JsonProperty("status_message")
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
