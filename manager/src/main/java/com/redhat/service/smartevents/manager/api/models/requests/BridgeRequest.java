package com.redhat.service.smartevents.manager.api.models.requests;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.api.user.validators.processors.ValidErrorHandler;
import com.redhat.service.smartevents.manager.models.Bridge;

@ValidErrorHandler
public class BridgeRequest {

    @NotEmpty(message = "Bridge name cannot be null or empty")
    @JsonProperty("name")
    private String name;

    @JsonProperty("errorHandler")
    @Valid
    private Action errorHandler;

    public BridgeRequest() {
    }

    public BridgeRequest(String name) {
        this.name = name;
    }

    public Bridge toEntity() {
        return new Bridge(name);
    }

    public String getName() {
        return name;
    }

    public Action getErrorHandler() {
        return errorHandler;
    }
}
