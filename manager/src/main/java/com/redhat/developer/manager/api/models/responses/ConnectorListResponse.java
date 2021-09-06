package com.redhat.developer.manager.api.models.responses;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorListResponse extends ListResponse {

    @JsonProperty("kind")
    private String kind = "ConnectorList";

    @JsonProperty("items")
    private List<ConnectorResponse> items = new ArrayList<>();

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<ConnectorResponse> getItems() {
        return items;
    }

    public void setItems(List<ConnectorResponse> items) {
        this.items = items;
    }
}
