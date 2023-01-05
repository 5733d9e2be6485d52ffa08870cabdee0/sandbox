package com.redhat.service.smartevents.infra.v2.api.models.queries;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.QueryParam;

import com.redhat.service.smartevents.infra.core.models.queries.BaseQueryFilterInfo;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.FILTER_STATUS;

public class QueryFilterInfo extends BaseQueryFilterInfo {

    @QueryParam(FILTER_STATUS)
    private Set<ManagedResourceStatusV2> filterStatus;

    protected QueryFilterInfo() {
    }

    protected QueryFilterInfo(String filterName, Set<ManagedResourceStatusV2> filterStatus) {
        super(filterName);
        this.filterStatus = filterStatus;
    }

    public static class QueryFilterInfoBuilder {

        public static QueryFilterInfoBuilder filter() {
            return new QueryFilterInfoBuilder();
        }

        private String filterName;
        private Set<ManagedResourceStatusV2> filterStatus = new HashSet<>();

        public QueryFilterInfoBuilder by(String filterName) {
            this.filterName = filterName;
            return this;
        }

        public QueryFilterInfoBuilder by(ManagedResourceStatusV2 filterStatus) {
            this.filterStatus.add(filterStatus);
            return this;
        }

        public QueryFilterInfo build() {
            return new QueryFilterInfo(filterName, filterStatus);
        }

    }

    public Set<ManagedResourceStatusV2> getFilterStatus() {
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
