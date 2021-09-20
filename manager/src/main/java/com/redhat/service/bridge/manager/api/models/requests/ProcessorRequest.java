package com.redhat.service.bridge.manager.api.models.requests;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;

public class ProcessorRequest {

    @NotEmpty(message = "Processor name cannot be null or empty")
    @JsonProperty("name")
    private String name;

    @JsonProperty("filters")
    private Set<BaseFilter> filters;

    public ProcessorRequest() {
    }

    public ProcessorRequest(String name) {
        this.name = name;
        this.filters = new HashSet<>();
    }

    public ProcessorRequest(String name, Set<BaseFilter> filters) {
        this.name = name;
        this.filters = filters;
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
}
