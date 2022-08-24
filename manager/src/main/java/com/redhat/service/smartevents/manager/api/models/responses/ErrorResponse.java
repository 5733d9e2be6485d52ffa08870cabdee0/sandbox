package com.redhat.service.smartevents.manager.api.models.responses;

import java.time.ZonedDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
    @JsonProperty("recorded_at")
    private ZonedDateTime recordedAt;

    @JsonProperty("headers")
    protected Map<String, String> headers;

    @JsonProperty("payload")
    protected JsonNode payload;

    public ZonedDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(ZonedDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}
