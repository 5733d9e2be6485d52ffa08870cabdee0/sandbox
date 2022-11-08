package com.redhat.service.smartevents.manager.v1.api.models.responses;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessorCatalogResponse {

    @NotNull
    @JsonProperty("kind")
    private final String kind = "SchemaCatalog";

    @JsonProperty("items")
    private List<ProcessorSchemaEntryResponse> items;

    public ProcessorCatalogResponse() {
    }

    public ProcessorCatalogResponse(List<ProcessorSchemaEntryResponse> items) {
        this.items = items;
    }

    public List<ProcessorSchemaEntryResponse> getItems() {
        return items;
    }
}
