package com.redhat.service.smartevents.infra.v1.api.models.queries;

import java.util.Objects;

import javax.ws.rs.BeanParam;

import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;

import static com.redhat.service.smartevents.infra.v1.api.models.queries.QueryProcessorFilterInfo.QueryProcessorFilterInfoBuilder.filter;

public class QueryProcessorResourceInfo extends QueryPageInfo {

    @BeanParam
    private QueryProcessorFilterInfo filterInfo;

    public QueryProcessorResourceInfo() {
        this(0, Integer.MAX_VALUE);
    }

    public QueryProcessorResourceInfo(int pageNumber, int pageSize) {
        this(pageNumber, pageSize, filter().build());
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
