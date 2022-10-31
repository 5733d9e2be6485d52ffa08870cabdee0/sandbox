package com.redhat.service.smartevents.infra.models.gateways;

import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

import static com.redhat.service.smartevents.infra.utils.JacksonUtils.mapToObjectNode;

public class Action {
    @NotNull(message = "An action type must be specified")
    @JsonProperty("type")
    private String type;

    @JsonProperty("parameters")
    // ObjectNode is not rendered properly by swagger
    @Schema(implementation = Object.class, required = true)
    private ObjectNode parameters;

    public ProcessorType getProcessorType() {
        return ProcessorType.SINK;
    }

    // mapParameters is included in openapi.yaml. We have to exclude it manually
    @Schema(hidden = true)
    public void setMapParameters(Map<String, String> parameters) {
        this.parameters = mapToObjectNode(parameters);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ObjectNode getParameters() {
        return parameters;
    }

    public void setParameters(ObjectNode parameters) {
        this.parameters = parameters;
    }

    public String getParameter(String key) {
        if (getParameters() == null) {
            return null;
        }

        JsonNode jsonNode = getParameters().get(key);
        if (jsonNode == null) {
            return null;
        }

        if (jsonNode instanceof TextNode) {
            return jsonNode.asText();
        }
        return jsonNode.toString();
    }

    public String getParameterOrDefault(String key, String defaultValue) {
        if (parameters == null) {
            return defaultValue;
        }

        String obj = getParameter(key);
        if (obj == null) {
            return defaultValue;
        }
        return obj;
    }

    public boolean hasParameter(String key) {
        return getParameters().get(key) != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Action that = (Action) o;
        return Objects.equals(type, that.type) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, parameters);
    }
}
