package com.redhat.service.bridge.manager.api.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ListResponse {

    @JsonProperty("page")
    private long page;

    @JsonProperty("size")
    private long size;

    @JsonProperty("total")
    private long total;

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
