package com.redhat.service.smartevents.infra.api.models.responses;

import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.ListResult;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class PagedListResponse<T> extends ListResponse<T> {

    public static <T, V> PagedListResponse<V> fill(ListResult<T> source, PagedListResponse<V> target, Function<T, V> converter) {
        target.setItems(source.getItems().stream().map(converter).collect(Collectors.toList()));
        target.setPage(source.getPage());
        target.setSize(source.getSize());
        target.setTotal(source.getTotal());
        return target;
    }

    protected PagedListResponse(String kind) {
        super(kind);
    }

    @NotNull
    @JsonProperty("page")
    private long page;

    @NotNull
    @JsonProperty("size")
    private long size;

    @NotNull
    @JsonProperty("total")
    private long total;

    /**
     * Gets the page number.
     * 
     * @return The page number
     */
    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    /**
     * Gets the number of items *after* any potential filtering has been applied. {@see QueryFilterInfo}.
     * This is identical to {@see ListResponse#getItems().size()}.
     * 
     * @return The number of items
     */
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Gets the total number of items *before* any potential filtering has been applied. {@see QueryFilterInfo}.
     * 
     * @return The total number of items.
     */
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
