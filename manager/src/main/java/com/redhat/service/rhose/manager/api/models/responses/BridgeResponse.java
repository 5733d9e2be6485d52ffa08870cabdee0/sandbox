package com.redhat.service.rhose.manager.api.models.responses;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.rhose.infra.api.models.responses.BaseResponse;
import com.redhat.service.rhose.infra.models.dto.ManagedResourceStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BridgeResponse extends BaseResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
    @JsonProperty("submitted_at")
    private ZonedDateTime submittedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
    @JsonProperty("published_at")
    private ZonedDateTime publishedAt;

    @JsonProperty("status")
    private ManagedResourceStatus status;

    @JsonProperty("endpoint")
    private String endpoint;

    public BridgeResponse() {
        super("Bridge");
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public ZonedDateTime getPublishedAt() {
        return publishedAt;
    }

    public ManagedResourceStatus getStatus() {
        return status;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public void setPublishedAt(ZonedDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setStatus(ManagedResourceStatus status) {
        this.status = status;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
