package com.redhat.service.smartevents.manager.v2.api.user.models.requests;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConnectorRequest {

    @NotEmpty(message = "Connector name cannot be null or empty")
    @JsonProperty("name")
    @Schema(description = "The name of the connector", example = "my-connector")
    protected String name;

    @NotEmpty(message = "Connector type cannot be null or empty")
    @JsonProperty("connector_type_id")
    @Schema(description = "The name of the connector", example = "slack_sink_0.1")
    protected String connectorTypeId;

    @JsonProperty("connector")
    // ObjectNode is not rendered properly by swagger
    @Schema(implementation = Object.class, required = true, description = "The Connector configuration payload")
    @NotNull(message = "Connector payload can't be null")
    protected ObjectNode connector;

    public ConnectorRequest(){}

    public ConnectorRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

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

    public Connector toEntity() {
        return new Connector(getName());
    }
}
