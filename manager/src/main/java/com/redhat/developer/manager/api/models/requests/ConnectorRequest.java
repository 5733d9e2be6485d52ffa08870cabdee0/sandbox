package com.redhat.developer.manager.api.models.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.developer.manager.models.Connector;

public class ConnectorRequest {

    @JsonProperty("name")
    private String name;

    public ConnectorRequest() {
    }

    public ConnectorRequest(String name) {
        this.name = name;
    }

    public Connector toEntity() {
        Connector connector = new Connector(name);
        return connector;
    }

    public String getName() {
        return name;
    }
}
