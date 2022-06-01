package com.redhat.service.smartevents.processor.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessorCatalogEntry {

    @JsonProperty("name")
    String name;

    @JsonProperty("isConnector")
    boolean isConnector;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isConnector() {
        return isConnector;
    }

    public void setConnector(boolean connector) {
        isConnector = connector;
    }
}
