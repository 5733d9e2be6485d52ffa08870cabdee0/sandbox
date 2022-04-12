package com.redhat.service.rhose.manager.api.models.requests;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.rhose.manager.models.Bridge;

public class BridgeRequest {

    @NotEmpty(message = "Bridge name cannot be null or empty")
    @JsonProperty("name")
    private String name;

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
}
