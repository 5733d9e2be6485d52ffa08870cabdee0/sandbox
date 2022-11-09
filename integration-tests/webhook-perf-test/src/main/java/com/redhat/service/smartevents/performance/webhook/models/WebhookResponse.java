package com.redhat.service.smartevents.performance.webhook.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookResponse {

    @JsonProperty("endpoint")
    private String endpoint;

    public WebhookResponse() {
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

}