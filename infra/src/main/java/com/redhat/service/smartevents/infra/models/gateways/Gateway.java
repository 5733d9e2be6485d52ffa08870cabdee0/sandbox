package com.redhat.service.smartevents.infra.models.gateways;

import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

import static com.redhat.service.smartevents.infra.utils.JacksonUtils.mapToObjectNode;

/**
 * A Gateway represents the touching point between a Processor and
 * the external world. Events pass through it to flow in and out.
 * It is intended to be the common concept between {@link Source},
 * the gateway from which events get in the Processor, and
 * {@link Action}, the gateway from which events exit the Processor
 * towards the external world.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
// See https://issues.redhat.com/browse/MGDOBR-638
// Implementations *MUST* override equals(..) and hashCode() appropriately
public abstract class Gateway {

    @JsonProperty("name")
    private String name;

    @NotNull(message = "A gateway type must be specified")
    @JsonProperty("type")
    private String type;

    @JsonProperty("parameters")
    // ObjectNode is not rendered properly by swagger
    @Schema(implementation = Object.class, required = true)
    private ObjectNode parameters;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectNode getParameters() {
        return parameters;
    }

    public void setParameters(ObjectNode parameters) {
        this.parameters = parameters;
    }

    // mapParameters is included in openapi.yaml. We have to exclude it manually
    @Schema(hidden = true)
    public void setMapParameters(Map<String, String> parameters) {
        this.parameters = mapToObjectNode(parameters);
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

    @JsonIgnore
    public abstract ProcessorType getProcessorType();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Gateway gateway = (Gateway) o;
        return Objects.equals(name, gateway.name) && type.equals(gateway.type) && Objects.equals(parameters, gateway.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, parameters);
    }
}
