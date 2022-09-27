package com.redhat.service.smartevents.manager.api.v1.models.responses;

import java.util.Set;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class ProcessorResponse extends BaseManagedResourceResponse {

    public ProcessorResponse() {
        super("Processor");
    }

    @NotNull
    @JsonProperty("type")
    private ProcessorType type;

    @JsonProperty("filters")
    private Set<BaseFilter> filters;

    @JsonProperty("transformationTemplate")
    private String transformationTemplate;

    @JsonProperty("action")
    private Action action;

    @JsonProperty("source")
    private Source source;

    @JsonProperty("status_message")
    private String statusMessage;

    public ProcessorType getType() {
        return type;
    }

    public void setType(ProcessorType type) {
        this.type = type;
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

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

}
