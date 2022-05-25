package com.redhat.service.smartevents.manager.api.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessorSchemaEntryResponse {
    @JsonProperty("kind")
    private final String kind = "ProcessorSchemaEntry";

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("href")
    private String href;

    public ProcessorSchemaEntryResponse(String name, String type, String href) {
        this.name = name;
        this.type = type;
        this.href = href;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
