package com.redhat.service.smartevents.infra.models.processors;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;

public class ProcessorDefinition {

    @JsonProperty("actions")
    private List<Action> multipleActions;

    @JsonProperty("filters")
    private Set<BaseFilter> filters;

    @JsonProperty("transformationTemplate")
    private String transformationTemplate;

    @JsonProperty("requestedAction")
    private Action requestedAction;

    @JsonProperty("requestedSource")
    private Source requestedSource;

    @JsonProperty("resolvedAction")
    private Action resolvedAction;

    @JsonProperty("processing")
    private Processing processing;

    public ProcessorDefinition() {
    }

    public ProcessorDefinition(Set<BaseFilter> filters, String transformationTemplate, Action requestedAction) {
        this(filters, transformationTemplate, requestedAction, requestedAction);
    }

    public ProcessorDefinition(Set<BaseFilter> filters, String transformationTemplate, Action requestedAction, Action resolvedAction) {
        this.filters = filters;
        this.transformationTemplate = transformationTemplate;
        this.requestedAction = requestedAction;
        this.resolvedAction = resolvedAction;
    }

    public ProcessorDefinition(Set<BaseFilter> filters, String transformationTemplate, Source requestedSource, Action resolvedAction) {
        this.filters = filters;
        this.transformationTemplate = transformationTemplate;
        this.requestedSource = requestedSource;
        this.resolvedAction = resolvedAction;
    }

    public ProcessorDefinition(Set<BaseFilter> filters, String transformationTemplate,
            Action requestedAction,
            Action resolvedAction,
            Processing processing,
            List<Action> multipleActions) {
        this(filters, transformationTemplate, requestedAction, resolvedAction);
        this.processing = processing;
        this.multipleActions = multipleActions;
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

    public Action getRequestedAction() {
        return requestedAction;
    }

    public void setRequestedAction(Action requestedAction) {
        this.requestedAction = requestedAction;
    }

    public Source getRequestedSource() {
        return requestedSource;
    }

    public void setRequestedSource(Source requestedSource) {
        this.requestedSource = requestedSource;
    }

    public Action getResolvedAction() {
        return resolvedAction;
    }

    public void setResolvedAction(Action resolvedAction) {
        this.resolvedAction = resolvedAction;
    }

    public Processing getProcessing() {
        return processing;
    }

    public void setProcessing(Processing processing) {
        this.processing = processing;
    }

    public List<Action> getMultipleActions() {
        return multipleActions;
    }

    public void setMultipleActions(List<Action> multipleActions) {
        this.multipleActions = multipleActions;
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
        return Objects.equals(multipleActions, that.multipleActions) && Objects.equals(filters, that.filters) && Objects.equals(transformationTemplate, that.transformationTemplate)
                && Objects.equals(requestedAction, that.requestedAction) && Objects.equals(requestedSource, that.requestedSource) && Objects.equals(resolvedAction, that.resolvedAction)
                && Objects.equals(processing, that.processing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multipleActions, filters, transformationTemplate, requestedAction, requestedSource, resolvedAction, processing);
    }

    @Override
    public String toString() {
        return "ProcessorDefinition{" +
                "multipleActions=" + multipleActions +
                ", filters=" + filters +
                ", transformationTemplate='" + transformationTemplate + '\'' +
                ", requestedAction=" + requestedAction +
                ", requestedSource=" + requestedSource +
                ", resolvedAction=" + resolvedAction +
                ", processing=" + processing +
                '}';
    }
}
