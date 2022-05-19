package com.redhat.service.smartevents.infra.models.gateways;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
public class Gateway {

    @NotNull(message = "A gateway type must be specified")
    @JsonProperty("type")
    private String type;

    @JsonProperty("parameters_old")
    private Map<String, String> parameters = new HashMap<>();

    // TODO NotEmpty cannot be used on ObjectNode
    // @NotEmpty(message = "Gateway parameters must be supplied")
    @JsonProperty("parameters")
    private ObjectNode rawParameters;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public ObjectNode getRawParameters() {
        return rawParameters;
    }

    public void setRawParameters(ObjectNode rawParameters) {
        this.rawParameters = rawParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Gateway that = (Gateway) o;
        return Objects.equals(type, that.type) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, parameters);
    }
}
