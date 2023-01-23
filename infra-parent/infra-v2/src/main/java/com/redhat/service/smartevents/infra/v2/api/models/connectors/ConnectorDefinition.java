package com.redhat.service.smartevents.infra.v2.api.models.connectors;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConnectorDefinition {

    @JsonProperty("connector")
    @NotNull(message = "Connector definition cannot be null")
    // ObjectNode is not rendered properly by swagger
    @Schema(implementation = Object.class, required = true)
    private ObjectNode connector;

    public ConnectorDefinition() {
    }

    public ConnectorDefinition(ObjectNode connector) {
        this.connector = connector;
    }

    public ObjectNode getConnector() {
        return connector;
    }

    public void setConnector(ObjectNode connector) {
        this.connector = connector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectorDefinition that = (ConnectorDefinition) o;
        return Objects.equals(connector, that.connector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connector);
    }
}
