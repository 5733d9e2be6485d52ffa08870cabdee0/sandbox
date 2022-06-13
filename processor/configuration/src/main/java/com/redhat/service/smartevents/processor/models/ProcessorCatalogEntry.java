package com.redhat.service.smartevents.processor.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessorCatalogEntry {

    @JsonProperty("id")
    String id;

    @JsonProperty("isConnector")
    boolean isConnector;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isConnector() {
        return isConnector;
    }

    public void setConnector(boolean connector) {
        isConnector = connector;
    }
}
