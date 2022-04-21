package com.redhat.service.smartevents.infra.models.gateways;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class Gateway {

    @NotNull(message = "A gateway type must be specified")
    @JsonProperty("type")
    private String type;

    @NotEmpty(message = "Gateway parameters must be supplied")
    @JsonProperty("parameters")
    private Map<String, String> parameters = new HashMap<>();

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
