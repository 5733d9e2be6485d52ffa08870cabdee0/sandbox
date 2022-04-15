package com.redhat.service.smartevents.infra.api.models.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.ListResult;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ListResponse<T> {

    public static <T, V> ListResponse<V> fill(ListResult<T> source, ListResponse<V> target, Function<T, V> converter) {
        target.setItems(source.getItems().stream().map(converter).collect(Collectors.toList()));
        target.setPage(source.getPage());
        target.setSize(source.getSize());
        target.setTotal(source.getTotal());
        return target;
    }

    protected ListResponse(String kind) {
        this.kind = kind;
    }

    @JsonProperty("kind")
    private final String kind;

    @JsonProperty("items")
    private List<T> items = new ArrayList<>();

    @JsonProperty("page")
    private long page;

    @JsonProperty("size")
    private long size;

    @JsonProperty("total")
    private long total;

    public String getKind() {
        return kind;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

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
