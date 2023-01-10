package com.redhat.service.smartevents.infra.v1.api.models.queries;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.QueryParam;

import com.redhat.service.smartevents.infra.core.models.queries.BaseQueryFilterInfo;
import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.FILTER_STATUS;

public class QueryFilterInfo extends BaseQueryFilterInfo {

    @QueryParam(FILTER_STATUS)
    private Set<ManagedResourceStatusV1> filterStatus;

    protected QueryFilterInfo() {
    }

    protected QueryFilterInfo(String filterName, Set<ManagedResourceStatusV1> filterStatus) {
        super(filterName);
        this.filterStatus = filterStatus;
    }

    public static class QueryFilterInfoBuilder {

        public static QueryFilterInfoBuilder filter() {
            return new QueryFilterInfoBuilder();
        }

        private String filterName;
        private Set<ManagedResourceStatusV1> filterStatus = new HashSet<>();

        public QueryFilterInfoBuilder by(String filterName) {
            this.filterName = filterName;
            return this;
        }

        public QueryFilterInfoBuilder by(ManagedResourceStatusV1 filterStatus) {
            this.filterStatus.add(filterStatus);
            return this;
        }

        public QueryFilterInfo build() {
            return new QueryFilterInfo(filterName, filterStatus);
        }

    }

    public Set<ManagedResourceStatusV1> getFilterStatus() {
        return filterStatus;
    }

    @Override
    public String toString() {
        return "QueryFilterInfo{" +
                "filterName='" + getFilterName() + '\'' +
                ", filterStatus=" + filterStatus +
                '}';
    }

}
