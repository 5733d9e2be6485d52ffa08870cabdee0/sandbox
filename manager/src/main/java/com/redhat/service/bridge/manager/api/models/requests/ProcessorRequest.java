package com.redhat.service.bridge.manager.api.models.requests;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;

public class ProcessorRequest {

    @NotEmpty(message = "Processor name cannot be null or empty")
    @JsonProperty("name")
    private String name;

    @NotNull(message = "An Action is required for a Processor")
    @JsonProperty("action")
    private BaseAction baseAction;

    @JsonProperty("filters")
    private Set<BaseFilter> filters;

    public ProcessorRequest() {
    }

    public ProcessorRequest(String name, BaseAction baseAction) {
        this.name = name;
        this.filters = new HashSet<>();
        this.baseAction = baseAction;
    }

    public ProcessorRequest(String name, Set<BaseFilter> filters, BaseAction baseAction) {
        this.name = name;
        this.filters = filters;
        this.baseAction = baseAction;
    }

    public String getName() {
        return name;
    }

    public Set<BaseFilter> getFilters() {
        return filters;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFilters(Set<BaseFilter> filters) {
        this.filters = filters;
    }

    public BaseAction getAction() {
        return baseAction;
    }

    public void setAction(BaseAction baseAction) {
        this.baseAction = baseAction;
    }
}
