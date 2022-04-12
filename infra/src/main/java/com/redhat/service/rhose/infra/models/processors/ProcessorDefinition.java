package com.redhat.service.rhose.infra.models.processors;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.rhose.infra.models.actions.BaseAction;
import com.redhat.service.rhose.infra.models.filters.BaseFilter;

public class ProcessorDefinition {

    @JsonProperty("filters")
    private Set<BaseFilter> filters;

    @JsonProperty("transformationTemplate")
    private String transformationTemplate;

    @JsonProperty("requestedAction")
    private BaseAction requestedAction;

    @JsonProperty("resolvedAction")
    private BaseAction resolvedAction;

    public ProcessorDefinition() {
    }

    public ProcessorDefinition(Set<BaseFilter> filters, String transformationTemplate, BaseAction requestedAction) {
        this(filters, transformationTemplate, requestedAction, requestedAction);
    }

    public ProcessorDefinition(Set<BaseFilter> filters, String transformationTemplate, BaseAction requestedAction, BaseAction resolvedAction) {
        this.filters = filters;
        this.transformationTemplate = transformationTemplate;
        this.requestedAction = requestedAction;
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

    public BaseAction getRequestedAction() {
        return requestedAction;
    }

    public void setRequestedAction(BaseAction requestedAction) {
        this.requestedAction = requestedAction;
    }

    public BaseAction getResolvedAction() {
        return resolvedAction;
    }

    public void setResolvedAction(BaseAction resolvedAction) {
        this.resolvedAction = resolvedAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProcessorDefinition that = (ProcessorDefinition) o;
        return Objects.equals(filters, that.filters) && Objects.equals(transformationTemplate, that.transformationTemplate) && Objects.equals(requestedAction, that.requestedAction)
                && Objects.equals(resolvedAction, that.resolvedAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filters, transformationTemplate, requestedAction, resolvedAction);
    }
}
