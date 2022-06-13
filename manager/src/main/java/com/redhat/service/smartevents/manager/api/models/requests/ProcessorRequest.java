package com.redhat.service.smartevents.manager.api.models.requests;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.api.user.validators.processors.ValidProcessorGateway;
import com.redhat.service.smartevents.manager.api.user.validators.processors.ValidTransformationTemplate;

@ValidProcessorGateway
public class ProcessorRequest {

    @NotEmpty(message = "Processor name cannot be null or empty")
    @JsonProperty("name")
    private String name;

    @JsonProperty("filters")
    private Set<@Valid BaseFilter> filters;

    @JsonProperty("transformationTemplate")
    @ValidTransformationTemplate
    private String transformationTemplate;

    @JsonProperty("action")
    @Valid
    private Action action;

    @JsonProperty("source")
    @Valid
    private Source source;

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
        throw new IllegalStateException("ProcessorRequest with unknown type");
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

    public void setFilters(Set<BaseFilter> filters) {
        this.filters = filters;
    }

    public String getTransformationTemplate() {
        return transformationTemplate;
    }

    public void setTransformationTemplate(String transformationTemplate) {
        this.transformationTemplate = transformationTemplate;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    @JsonIgnore
    public Gateway getGateway() {
        if (action != null) {
            return action;
        }
        return source;
    }
}
