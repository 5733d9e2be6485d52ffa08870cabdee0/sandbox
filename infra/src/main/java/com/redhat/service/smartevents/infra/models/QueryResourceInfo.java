package com.redhat.service.smartevents.infra.models;

import java.util.Objects;

import javax.ws.rs.BeanParam;

public class QueryResourceInfo extends QueryPageInfo {

    @BeanParam
    private QueryFilterInfo filterInfo;

    public QueryResourceInfo() {
    }

    public QueryResourceInfo(int pageNumber, int pageSize) {
        this(pageNumber, pageSize, new QueryFilterInfo());
    }

    public QueryResourceInfo(int pageNumber, int pageSize, QueryFilterInfo filterInfo) {
        super(pageNumber, pageSize);
        this.filterInfo = Objects.requireNonNull(filterInfo);
    }

    public QueryFilterInfo getFilterInfo() {
        return filterInfo;
    }

    @Override
    public String toString() {
        return "QueryInfo{" +
                "pageNumber=" + super.getPageNumber() +
                ", pageSize=" + super.getPageSize() +
                ", filterInfo=" + filterInfo +
                '}';
    }
}
