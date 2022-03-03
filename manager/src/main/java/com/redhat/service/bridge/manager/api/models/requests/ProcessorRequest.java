package com.redhat.service.bridge.manager.api.models.requests;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;

public class ProcessorRequest {

    @NotEmpty(message = "Processor name cannot be null or empty")
    @JsonProperty("name")
    private String name;

    @JsonProperty("filters")
    private Set<@Valid BaseFilter> filters;

    @JsonProperty("transformationTemplate")
    private String transformationTemplate;

    @NotNull(message = "An Action is required for a Processor")
    @JsonProperty("action")
    @Valid
    private BaseAction baseAction;

    public ProcessorRequest() {
    }

    public ProcessorRequest(String name, BaseAction baseAction) {
        this.name = name;
        this.baseAction = baseAction;
    }

    public ProcessorRequest(String name, Set<BaseFilter> filters, String transformationTemplate, BaseAction baseAction) {
        this.name = name;
        this.filters = filters;
        this.transformationTemplate = transformationTemplate;
        this.baseAction = baseAction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<BaseFilter> getFilters() {
        return filters;
    }

    public String getTransformationTemplate() {
        return transformationTemplate;
    }

    public BaseAction getAction() {
        return baseAction;
    }

    public void setAction(BaseAction baseAction) {
        this.baseAction = baseAction;
    }
}
