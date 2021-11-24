package com.redhat.service.bridge.infra.models.processors;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;

public class ProcessorDefinition {

    @JsonProperty("filters")
    private Set<BaseFilter> filters;

    @JsonProperty("transformationTemplate")
    private String transformationTemplate;

    @JsonProperty("action")
    private BaseAction action;

    @JsonProperty("resolvedAction")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private BaseAction resolvedAction;

    public ProcessorDefinition() {
    }

    public ProcessorDefinition(Set<BaseFilter> filters, String transformationTemplate, BaseAction action) {
        this.filters = filters;
        this.transformationTemplate = transformationTemplate;
        this.action = action;
    }

    public ProcessorDefinition(Set<BaseFilter> filters, String transformationTemplate, BaseAction action, BaseAction resolvedAction) {
        this(filters, transformationTemplate, action);
        this.resolvedAction = resolvedAction;
    }

    public Set<BaseFilter> getFilters() {
        return filters;
    }

    public void setFilters(Set<BaseFilter> filters) {
        this.filters = filters;
    }

    public String getTransformationTemplate() {
        return transformationTemplate;
    }

    public void setTransformationTemplate(String transformationTemplate) {
        this.transformationTemplate = transformationTemplate;
    }

    public BaseAction getAction() {
        return action;
    }

    public void setAction(BaseAction action) {
        this.action = action;
    }

    public BaseAction getResolvedAction() {
        return resolvedAction;
    }

    public void setResolvedAction(BaseAction resolvedAction) {
        this.resolvedAction = resolvedAction;
    }

    @JsonIgnore
    public BaseAction getExecutableAction() {
        return resolvedAction == null ? action : resolvedAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProcessorDefinition processor = (ProcessorDefinition) o;
        return Objects.equals(filters, processor.filters) && Objects.equals(transformationTemplate, processor.transformationTemplate)
                && Objects.equals(action, processor.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filters, transformationTemplate, action);
    }
}
