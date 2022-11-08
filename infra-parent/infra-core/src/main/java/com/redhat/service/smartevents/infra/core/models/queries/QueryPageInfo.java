package com.redhat.service.smartevents.infra.core.models.queries;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.PAGE;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.PAGE_DEFAULT;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.PAGE_MIN;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.PAGE_SIZE;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.SIZE_DEFAULT;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.SIZE_MAX;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.SIZE_MIN;

public class QueryPageInfo {

    @DefaultValue(PAGE_DEFAULT)
    @Min(PAGE_MIN)
    @QueryParam(PAGE)
    private int pageNumber;
    @DefaultValue(SIZE_DEFAULT)
    @Min(SIZE_MIN)
    @Max(SIZE_MAX)
    @QueryParam(PAGE_SIZE)
    private int pageSize;

    public QueryPageInfo() {
    }

    public QueryPageInfo(int pageNumber, int pageSize) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    @Override
    public String toString() {
        return "QueryInfo [pageNumber=" + pageNumber + ", pageSize=" + pageSize + "]";
    }

}
