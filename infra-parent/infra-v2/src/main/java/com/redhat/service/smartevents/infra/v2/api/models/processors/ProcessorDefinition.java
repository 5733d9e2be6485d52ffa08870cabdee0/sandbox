package com.redhat.service.smartevents.infra.v2.api.models.processors;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProcessorDefinition {

    @JsonProperty("flows")
    @NotNull(message = "Processor flows cannot be null")
    // ObjectNode is not rendered properly by swagger
    @Schema(implementation = Object.class, required = true)
    private ObjectNode flows;

    public ProcessorDefinition() {
    }

    public ProcessorDefinition(ObjectNode flows) {
        this.flows = flows;
    }

    public ObjectNode getFlows() {
        return flows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProcessorDefinition)) {
            return false;
        }
        ProcessorDefinition that = (ProcessorDefinition) o;
        return getFlows().equals(that.getFlows());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFlows());
    }
}
