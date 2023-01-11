package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.manager.core.api.models.responses.BaseManagedResourceResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class ProcessorResponse extends BaseManagedResourceResponse<ManagedResourceStatusV2> {

    public ProcessorResponse() {
        super("Processor");
    }

    @JsonProperty("flows")
    // ObjectNode is not rendered properly by swagger
    @Schema(implementation = Object.class, required = true,
            description = "The Camel YAML DSL code, formatted as JSON, that defines the flows in the processor")
    @NotNull
    private ObjectNode flows;

    @JsonProperty("status_message")
    private String statusMessage;

    public ObjectNode getFlows() {
        return flows;
    }

    public void setFlows(ObjectNode flows) {
        this.flows = flows;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

}
