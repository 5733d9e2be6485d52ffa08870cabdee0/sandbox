package com.redhat.service.bridge.infra.models.actions;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionRequest {

    @NotNull(message = "An Action must have a name")
    @JsonProperty("name")
    private String name;

    @NotNull(message = "An Action Type must be specified")
    @JsonProperty("type")
    private String type;

    @NotEmpty(message = "Action parameters must be supplied")
    @JsonProperty("parameters")
    private Map<String, String> parameters = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
}
