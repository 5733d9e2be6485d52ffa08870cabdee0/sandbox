package com.redhat.service.smartevents.infra.models;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.QueryParam;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

import static com.redhat.service.smartevents.infra.api.APIConstants.FILTER_PROCESSOR_TYPE;

public class QueryProcessorFilterInfo extends QueryFilterInfo {

    @QueryParam(FILTER_PROCESSOR_TYPE)
    private ProcessorType filterType;

    public static class QueryProcessorFilterInfoBuilder {

        public static QueryProcessorFilterInfo.QueryProcessorFilterInfoBuilder filter() {
            return new QueryProcessorFilterInfo.QueryProcessorFilterInfoBuilder();
        }

        private String filterName;
        private Set<ManagedResourceStatus> filterStatus = new HashSet<>();
        private ProcessorType filterType;

        public QueryProcessorFilterInfoBuilder by(String filterName) {
            this.filterName = filterName;
            return this;
        }

        public QueryProcessorFilterInfoBuilder by(ManagedResourceStatus filterStatus) {
            this.filterStatus.add(filterStatus);
            return this;
        }

        public QueryProcessorFilterInfo.QueryProcessorFilterInfoBuilder by(ProcessorType filterType) {
            this.filterType = filterType;
            return this;
        }

        public QueryProcessorFilterInfo build() {
            return new QueryProcessorFilterInfo(filterName, filterStatus, filterType);
        }

    }

    protected QueryProcessorFilterInfo() {

    }

    protected QueryProcessorFilterInfo(String filterName, Set<ManagedResourceStatus> filterStatus, ProcessorType filterType) {
        super(filterName, filterStatus);
        this.filterType = filterType;
    }

    public ProcessorType getFilterType() {
        return filterType;
    }

    @Override
    public String toString() {
        return "QueryProcessorFilterInfo{" +
                "filterNamePrefix='" + getFilterNamePrefix() + '\'' +
                ", filterStatus=" + getFilterStatus() +
                ", filterType=" + filterType +
                '}';
    }
}
