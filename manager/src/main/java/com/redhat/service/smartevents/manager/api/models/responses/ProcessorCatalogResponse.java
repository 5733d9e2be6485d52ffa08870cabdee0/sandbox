package com.redhat.service.smartevents.manager.api.models.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessorCatalogResponse {
    @JsonProperty("kind")
    private final String kind = "SchemaCatalog";

    @JsonProperty("items")
    private final List<ProcessorSchemaEntryResponse> items;

    public ProcessorCatalogResponse(List<ProcessorSchemaEntryResponse> items) {
        this.items = items;
    }

    public List<ProcessorSchemaEntryResponse> getItems() {
        return items;
    }
}
