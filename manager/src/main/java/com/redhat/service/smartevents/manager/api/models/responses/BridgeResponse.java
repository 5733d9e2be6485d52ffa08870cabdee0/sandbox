package com.redhat.service.smartevents.manager.api.models.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.gateways.Action;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BridgeResponse extends BaseManagedResourceResponse {

    @NotNull
    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("error_handler")
    private Action errorHandler;

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
}
