package com.redhat.developer.manager.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.developer.manager.models.Connector;

public class ConnectorRequest {

    @JsonProperty("name")
    private String name;

    public Connector toEntity() {
        Connector connector = new Connector(name);
        return connector;
    }

    public String getName() {
        return name;
    }
}
