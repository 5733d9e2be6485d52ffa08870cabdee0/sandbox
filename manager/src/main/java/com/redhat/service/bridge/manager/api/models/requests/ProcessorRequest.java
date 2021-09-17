package com.redhat.service.bridge.manager.api.models.requests;

import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.filters.Filter;

public class ProcessorRequest {

    @NotEmpty(message = "Processor name cannot be null or empty")
    @JsonProperty("name")
    private String name;

    @JsonProperty("filters")
    private Set<Filter> filters;

    public ProcessorRequest() {
    }

    public ProcessorRequest(String name, Set<Filter> filters) {
        this.name = name;
        this.filters = filters;
    }

    public String getName() {
        return name;
    }

    public Set<Filter> getFilters() {
        return filters;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFilters(Set<Filter> filters) {
        this.filters = filters;
    }
}
