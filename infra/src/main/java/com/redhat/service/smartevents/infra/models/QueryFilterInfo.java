package com.redhat.service.smartevents.infra.models;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.QueryParam;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;

import static com.redhat.service.smartevents.infra.api.APIConstants.FILTER_STATUS;
import static com.redhat.service.smartevents.infra.api.APIConstants.PREFIX;

public class QueryFilterInfo {

    @QueryParam(PREFIX)
    private String filterPrefix;

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

    protected QueryFilterInfo(String filterPrefix, Set<ManagedResourceStatus> filterStatus) {
        this.filterPrefix = filterPrefix;
        this.filterStatus = filterStatus;
    }

    public String getFilterPrefix() {
        return filterPrefix;
    }

    public Set<ManagedResourceStatus> getFilterStatus() {
        return filterStatus;
    }

    @Override
    public String toString() {
        return "QueryFilterInfo{" +
                "filterPrefix='" + filterPrefix + '\'' +
                ", filterStatus=" + filterStatus +
                '}';
    }
}
