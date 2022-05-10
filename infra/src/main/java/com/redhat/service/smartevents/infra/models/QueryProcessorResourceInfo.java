package com.redhat.service.smartevents.infra.models;

import java.util.Objects;

import javax.ws.rs.BeanParam;

public class QueryProcessorResourceInfo extends QueryPageInfo {

    @BeanParam
    private QueryProcessorFilterInfo filterInfo;

    public QueryProcessorResourceInfo() {
    }

    public QueryProcessorResourceInfo(int pageNumber, int pageSize) {
        this(pageNumber, pageSize, new QueryProcessorFilterInfo());
    }

    public QueryProcessorResourceInfo(int pageNumber, int pageSize, QueryProcessorFilterInfo filterInfo) {
        super(pageNumber, pageSize);
        this.filterInfo = Objects.requireNonNull(filterInfo);
    }

    public QueryProcessorFilterInfo getFilterInfo() {
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
