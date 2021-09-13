package com.redhat.service.bridge.manager.api.models.responses;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BridgeListResponse extends ListResponse {

    @JsonProperty("kind")
    private String kind = "BridgeList";

    @JsonProperty("items")
    private List<BridgeResponse> items = new ArrayList<>();

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<BridgeResponse> getItems() {
        return items;
    }

    public void setItems(List<BridgeResponse> items) {
        this.items = items;
    }
}
