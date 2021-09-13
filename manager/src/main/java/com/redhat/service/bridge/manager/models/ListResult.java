package com.redhat.service.bridge.manager.models;

import java.util.List;

/**
 * Encapsulates the result of calling a DAO method than returns a list. It not only includes
 * the results of the query, but also details on the total number of items, the page and number of items
 * on the page.
 *
 * @param <T> - The type of the entity returned in the list.
 */
public class ListResult<T> {

    private final List<T> items;

    private final long total;

    private final long page;

    public ListResult(List<T> items, long page, long total) {
        this.items = items;
        this.page = page;
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public long getSize() {
        return this.items == null ? 0 : items.size();
    }

    public long getTotal() {
        return total;
    }

    public List<T> getItems() {
        return items;
    }
}