package com.redhat.service.bridge.manager.api.models.requests;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.models.actions.ActionRequest;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;

public class ProcessorRequest {

    @NotEmpty(message = "Processor name cannot be null or empty")
    @JsonProperty("name")
    private String name;

    @NotNull(message = "An Action is required for a Processor")
    @JsonProperty("action")
    private ActionRequest actionRequest;

    @JsonProperty("filters")
    private Set<BaseFilter> filters;

    public ProcessorRequest() {
    }

    public ProcessorRequest(String name, ActionRequest actionRequest) {
        this.name = name;
        this.filters = new HashSet<>();
        this.actionRequest = actionRequest;
    }

    public ProcessorRequest(String name, Set<BaseFilter> filters, ActionRequest actionRequest) {
        this.name = name;
        this.filters = filters;
        this.actionRequest = actionRequest;
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

    public ActionRequest getAction() {
        return actionRequest;
    }

    public void setAction(ActionRequest actionRequest) {
        this.actionRequest = actionRequest;
    }
}
