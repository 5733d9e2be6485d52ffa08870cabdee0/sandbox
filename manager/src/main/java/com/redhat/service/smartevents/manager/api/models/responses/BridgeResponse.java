package com.redhat.service.smartevents.manager.api.models.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BridgeResponse extends BaseManagedResourceResponse {

    @JsonProperty("endpoint")
    private String endpoint;

    public BridgeResponse() {
        super("Bridge");
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
