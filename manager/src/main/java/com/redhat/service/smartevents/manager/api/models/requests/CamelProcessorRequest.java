package com.redhat.service.smartevents.manager.api.models.requests;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class CamelProcessorRequest {

    @NotEmpty(message = "Processor name cannot be null or empty")
    @JsonProperty("name")
    protected String name;

    @JsonProperty("flow")
    protected ArrayNode flow;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayNode getFlow() {
        return flow;
    }

    public void setFlow(ArrayNode flow) {
        this.flow = flow;
    }
}
