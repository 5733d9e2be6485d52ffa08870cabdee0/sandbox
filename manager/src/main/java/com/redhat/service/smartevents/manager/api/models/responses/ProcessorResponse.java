package com.redhat.service.smartevents.manager.api.models.responses;

import java.time.ZonedDateTime;
import java.util.Set;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.api.models.responses.BaseResponse;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class ProcessorResponse extends BaseResponse {

    public ProcessorResponse() {
        super("Processor");
    }

    @JsonProperty("type")
    private ProcessorType type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
    @JsonProperty("submitted_at")
    private ZonedDateTime submittedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
    @JsonProperty("published_at")
    private ZonedDateTime publishedAt;

    @JsonProperty("status")
    private ManagedResourceStatus status;

    @JsonProperty("filters")
    private Set<BaseFilter> filters;

    @JsonProperty("transformationTemplate")
    private String transformationTemplate;

    @JsonProperty("action")
    private Action action;

    @JsonProperty("source")
    private Source source;

    public ProcessorType getType() {
        return type;
    }

    public void setType(ProcessorType type) {
        this.type = type;
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public ZonedDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(ZonedDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public ManagedResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ManagedResourceStatus status) {
        this.status = status;
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
}
