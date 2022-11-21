package com.redhat.service.smartevents.manager.v2.api.user.models.requests;

import java.util.Objects;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

public class ProcessorRequest {

    @NotEmpty(message = "Processor name cannot be null or empty")
    @JsonProperty("name")
    protected String name;

    @JsonProperty("flows")
    @NotNull(message = "Processor flows cannot be null")
    // ObjectNode is not rendered properly by swagger
    @Schema(implementation = Object.class, required = true)
    private ObjectNode flows;

    public ProcessorRequest() {
    }

    public ProcessorRequest(String name) {
        this(name, new ObjectNode(JsonNodeFactory.instance));
    }

    public ProcessorRequest(String name, ObjectNode flows) {
        this.name = name;
        this.flows = flows;
    }

    public String getName() {
        return Objects.nonNull(name) ? name.trim() : null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectNode getFlows() {
        return flows;
    }

    public void setFlows(ObjectNode flows) {
        this.flows = flows;
    }

    public Processor toEntity() {
        return new Processor(getName());
    }

}
