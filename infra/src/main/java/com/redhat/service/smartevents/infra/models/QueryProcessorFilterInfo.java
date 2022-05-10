package com.redhat.service.smartevents.infra.models;

import java.util.Set;

import javax.ws.rs.QueryParam;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

import static com.redhat.service.smartevents.infra.api.APIConstants.FILTER_PROCESSOR_TYPE;

public class QueryProcessorFilterInfo extends QueryFilterInfo {

    @QueryParam(FILTER_PROCESSOR_TYPE)
    private ProcessorType filterType;

    public QueryProcessorFilterInfo() {
        this(null, (Set<ManagedResourceStatus>) null);
    }

    public QueryProcessorFilterInfo(String filterName) {
        this(filterName, (Set<ManagedResourceStatus>) null, null);
    }

    public QueryProcessorFilterInfo(ManagedResourceStatus filterStatus) {
        this(null, filterStatus, null);
    }

    public QueryProcessorFilterInfo(String filterName, ManagedResourceStatus filterStatus) {
        this(filterName, filterStatus, null);
    }

    public QueryProcessorFilterInfo(String filterName, Set<ManagedResourceStatus> filterStatus) {
        this(filterName, filterStatus, null);
    }

    public QueryProcessorFilterInfo(String filterName, ProcessorType filterType) {
        this(filterName, (Set<ManagedResourceStatus>) null, filterType);
    }

    public QueryProcessorFilterInfo(ManagedResourceStatus filterStatus, ProcessorType filterType) {
        this(null, filterStatus, filterType);
    }

    public QueryProcessorFilterInfo(Set<ManagedResourceStatus> filterStatus, ProcessorType filterType) {
        this(null, filterStatus, filterType);
    }

    public QueryProcessorFilterInfo(ProcessorType filterType) {
        this(null, (Set<ManagedResourceStatus>) null, filterType);
    }

    public QueryProcessorFilterInfo(String filterName, ManagedResourceStatus filterStatus, ProcessorType filterType) {
        super(filterName, filterStatus);
        this.filterType = filterType;
    }

    public QueryProcessorFilterInfo(String filterName, Set<ManagedResourceStatus> filterStatus, ProcessorType filterType) {
        super(filterName, filterStatus);
        this.filterType = filterType;
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
