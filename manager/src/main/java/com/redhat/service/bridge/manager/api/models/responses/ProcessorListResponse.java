package com.redhat.service.bridge.manager.api.models.responses;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessorListResponse extends ListResponse {

    @JsonProperty("kind")
    private String kind = "ProcessorList";

    @JsonProperty("items")
    private List<ProcessorResponse> items = new ArrayList<>();

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<ProcessorResponse> getItems() {
        return items;
    }

    public void setItems(List<ProcessorResponse> items) {
        this.items = items;
    }
}
