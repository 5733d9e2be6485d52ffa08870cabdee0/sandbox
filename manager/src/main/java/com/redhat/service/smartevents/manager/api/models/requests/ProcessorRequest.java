package com.redhat.service.smartevents.manager.api.models.requests;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.manager.api.user.validators.processors.ValidGateway;
import com.redhat.service.smartevents.manager.api.user.validators.processors.ValidTransformationTemplate;

@ValidGateway
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

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }
}
