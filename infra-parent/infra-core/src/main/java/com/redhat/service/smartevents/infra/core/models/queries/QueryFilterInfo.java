package com.redhat.service.smartevents.infra.core.models.queries;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.QueryParam;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.FILTER_NAME;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.FILTER_STATUS;

public class QueryFilterInfo {

    @QueryParam(FILTER_NAME)
    private String filterName;

    @QueryParam(FILTER_STATUS)
    private Set<ManagedResourceStatus> filterStatus;

    public static class QueryFilterInfoBuilder {

        public static QueryFilterInfoBuilder filter() {
            return new QueryFilterInfoBuilder();
        }

        private String filterName;
        private Set<ManagedResourceStatus> filterStatus = new HashSet<>();

        public QueryFilterInfoBuilder by(String filterName) {
            this.filterName = filterName;
            return this;
        }

        public QueryFilterInfoBuilder by(ManagedResourceStatus filterStatus) {
            this.filterStatus.add(filterStatus);
            return this;
        }

        public QueryFilterInfo build() {
            return new QueryFilterInfo(filterName, filterStatus);
        }

    }

    protected QueryFilterInfo() {

    }

    protected QueryFilterInfo(String filterName, Set<ManagedResourceStatus> filterStatus) {
        this.filterName = filterName;
        this.filterStatus = filterStatus;
    }

    public String getFilterName() {
        return filterName;
    }

    public Set<ManagedResourceStatus> getFilterStatus() {
        return filterStatus;
    }

    @Override
    public String toString() {
        return "QueryFilterInfo{" +
                "filterName='" + filterName + '\'' +
                ", filterStatus=" + filterStatus +
                '}';
    }
}
