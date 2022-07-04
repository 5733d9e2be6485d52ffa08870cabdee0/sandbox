package com.redhat.service.smartevents.processor.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessorCatalogEntry {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("isConnector")
    private boolean isConnector;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isConnector() {
        return isConnector;
    }

    public void setConnector(boolean connector) {
        isConnector = connector;
    }

    @Override
    public String toString() {
        return "ProcessorCatalogEntry{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isConnector=" + isConnector +
                '}';
    }
}
