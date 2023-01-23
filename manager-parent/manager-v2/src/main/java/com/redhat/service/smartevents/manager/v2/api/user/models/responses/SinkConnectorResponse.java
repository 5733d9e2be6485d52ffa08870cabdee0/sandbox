package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SinkConnectorResponse extends ConnectorResponse {

    @JsonProperty("uri_dsl")
    @NotNull
    @Schema(description = "The URI to be used in Camel DSL to send data to this sink", example = "knative:my-id")
    protected String uriDsl;

    public SinkConnectorResponse() {
        super("SinkConnector");
    }

    public String getUriDsl() {
        return uriDsl;
    }

    public void setUriDsl(String uriDsl) {
        this.uriDsl = uriDsl;
    }
}
