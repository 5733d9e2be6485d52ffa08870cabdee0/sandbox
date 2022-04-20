package com.redhat.service.smartevents.manager.api.models.requests;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.actions.Action;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;

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
    private Action action;

    public ProcessorRequest() {
    }

    public ProcessorRequest(String name, Action action) {
        this.name = name;
        this.action = action;
    }

    public ProcessorRequest(String name, Set<BaseFilter> filters, String transformationTemplate, Action action) {
        this.name = name;
        this.filters = filters;
        this.transformationTemplate = transformationTemplate;
        this.action = action;
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

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }
}
