package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.core.api.models.responses.BaseManagedResourceResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
// TODO: rename this class when https://github.com/redhat-developer/app-services-api-guidelines/issues/120 is fixed or when V1 is dropped.
public class BridgeResponseV2DTO extends BaseManagedResourceResponse {

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("cloud_provider")
    private String cloudProvider;

    @JsonProperty("region")
    private String region;

    @JsonProperty("status_message")
    private String statusMessage;

    public BridgeResponseV2DTO() {
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
