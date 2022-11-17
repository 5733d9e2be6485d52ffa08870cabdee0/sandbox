package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.manager.core.api.models.responses.BaseManagedResourceResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class ProcessorResponse extends BaseManagedResourceResponse {

    public ProcessorResponse() {
        super("Processor");
    }

    @JsonProperty("name")
    @NotNull
    protected String name;

    @JsonProperty("flows")
    // ObjectNode is not rendered properly by swagger
    @Schema(implementation = Object.class, required = true)
    @NotNull
    private ObjectNode flows;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public ObjectNode getFlows() {
        return flows;
    }

    public void setFlows(ObjectNode flows) {
        this.flows = flows;
    }
}
