package com.redhat.service.smartevents.manager.api.models.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.gateways.Action;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BridgeResponse extends BaseManagedResourceResponse {

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("error_handler")
    private Action errorHandler;

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

    public Action getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(Action errorHandler) {
        this.errorHandler = errorHandler;
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
