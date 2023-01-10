package com.redhat.service.smartevents.infra.v1.api.models.queries;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.QueryParam;

import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.FILTER_PROCESSOR_TYPE;

public class QueryProcessorFilterInfo extends QueryFilterInfo {

    @QueryParam(FILTER_PROCESSOR_TYPE)
    private ProcessorType filterType;

    protected QueryProcessorFilterInfo() {
    }

    protected QueryProcessorFilterInfo(String filterName, Set<ManagedResourceStatusV1> filterStatus, ProcessorType filterType) {
        super(filterName, filterStatus);
        this.filterType = filterType;
    }

    public static class QueryProcessorFilterInfoBuilder {

        public static QueryProcessorFilterInfoBuilder filter() {
            return new QueryProcessorFilterInfoBuilder();
        }

        private String filterName;
        private Set<ManagedResourceStatusV1> filterStatus = new HashSet<>();
        private ProcessorType filterType;

        public QueryProcessorFilterInfoBuilder by(String filterName) {
            this.filterName = filterName;
            return this;
        }

        public QueryProcessorFilterInfoBuilder by(ManagedResourceStatusV1 filterStatus) {
            this.filterStatus.add(filterStatus);
            return this;
        }

        public QueryProcessorFilterInfoBuilder by(ProcessorType filterType) {
            this.filterType = filterType;
            return this;
        }

        public QueryProcessorFilterInfo build() {
            return new QueryProcessorFilterInfo(filterName, filterStatus, filterType);
        }

    }

    public ProcessorType getFilterType() {
        return filterType;
    }

    @Override
    public String toString() {
        return "QueryProcessorFilterInfo{" +
                "filterName='" + getFilterName() + '\'' +
                ", filterStatus=" + getFilterStatus() +
                ", filterType=" + filterType +
                '}';
    }

}
