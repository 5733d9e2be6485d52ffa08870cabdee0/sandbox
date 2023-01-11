package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.manager.core.api.models.responses.BaseManagedResourceResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ConnectorResponse extends BaseManagedResourceResponse<ManagedResourceStatusV2> {

    @JsonProperty("name")
    @NotNull
    @Schema(description = "The name of the Connector", example = "my-connector")
    protected String name;

    @JsonProperty("connector_type_id")
    @NotNull
    @Schema(description = "The connector type", example = "slack_sink_0.1")
    protected String connectorTypeId;

    @JsonProperty("connector")
    // ObjectNode is not rendered properly by swagger
    @Schema(implementation = Object.class, required = true, description = "The Connector configuration payload")
    @NotNull
    private ObjectNode connector;

    @JsonProperty("status_message")
    @Schema(description = "A detailed status message in case there is a problem with the connector")
    private String statusMessage;

    protected ConnectorResponse(String kind) {
        super(kind);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getConnectorTypeId() {
        return connectorTypeId;
    }

    public void setConnectorTypeId(String connectorTypeId) {
        this.connectorTypeId = connectorTypeId;
    }

    public ObjectNode getConnector() {
        return connector;
    }

    public void setConnector(ObjectNode connector) {
        this.connector = connector;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
