package com.redhat.service.smartevents.infra.core.models.queries;

import javax.ws.rs.QueryParam;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.FILTER_NAME;

public abstract class BaseQueryFilterInfo {

    @QueryParam(FILTER_NAME)
    private String filterName;

    protected BaseQueryFilterInfo() {
    }

    protected BaseQueryFilterInfo(String filterName) {
        this.filterName = filterName;
    }

    public String getFilterName() {
        return filterName;
    }

    @Override
    public String toString() {
        return "QueryFilterInfo{" +
                "filterName='" + filterName + '\'' +
                '}';
    }

}
