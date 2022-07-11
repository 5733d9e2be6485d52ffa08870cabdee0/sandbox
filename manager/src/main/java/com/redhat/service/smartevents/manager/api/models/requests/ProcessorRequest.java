package com.redhat.service.smartevents.manager.api.models.requests;

import java.util.Objects;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.Processing;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.api.user.validators.processors.ValidProcessorGateway;
import com.redhat.service.smartevents.manager.api.user.validators.processors.ValidTransformationTemplate;

@ValidProcessorGateway
public class ProcessorRequest {

    @NotEmpty(message = "Processor name cannot be null or empty")
    @JsonProperty("name")
    protected String name;

    @JsonProperty("filters")
    @JsonDeserialize(using = FiltersDeserializer.class)
    protected Set<@Valid BaseFilter> filters;

    @JsonProperty("transformationTemplate")
    @ValidTransformationTemplate
    protected String transformationTemplate;

    @JsonProperty("action")
    @Valid
    protected Action action;

    @JsonProperty("actions")
    protected Set<Action> actions;

    @JsonProperty("source")
    @Valid
    protected Source source;

    @JsonProperty("processing")
    protected Processing processing;

    public ProcessorRequest() {
    }

    public ProcessorRequest(String name, Action action) {
        this.name = name;
        this.action = action;
    }

    public ProcessorRequest(String name, Source source) {
        this.name = name;
        this.source = source;
    }

    public ProcessorRequest(String name, Set<BaseFilter> filters, String transformationTemplate, Action action) {
        this.name = name;
        this.filters = filters;
        this.transformationTemplate = transformationTemplate;
        this.action = action;
    }

    @JsonIgnore
    public ProcessorType getType() {
        if (getSource() != null) {
            return ProcessorType.SOURCE;
        }
        if (getAction() != null) {
            return ProcessorType.SINK;
        }
        if (hasActions()) {
            return ProcessorType.SINK;
        }
        throw new IllegalStateException("ProcessorRequest with unknown type");
    }

    public String getName() {
        return Objects.nonNull(name) ? name.trim() : null;
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

    public Source getSource() {
        return source;
    }

    public Processing getProcessing() {
        return processing;
    }

    public Set<Action> getActions() {
        return actions;
    }

    public boolean hasActions() {
        return actions != null && actions.size() > 0;
    }

    @JsonIgnore
    public Gateway getGateway() {
        if (action != null) {
            return action;
        }
        return source;
    }

    @Override
    public String toString() {
        return "ProcessorRequest{" +
                "name='" + name + '\'' +
                ", filters=" + filters +
                ", transformationTemplate='" + transformationTemplate + '\'' +
                ", action=" + action +
                ", actions=" + actions +
                ", source=" + source +
                ", processing=" + processing +
                '}';
    }
}
