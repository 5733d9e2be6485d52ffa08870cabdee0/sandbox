package com.redhat.service.smartevents.manager.dao;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.models.Bridge;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class BridgeDAO implements PanacheRepositoryBase<Bridge, String> {

    private static final int FILTER_NAME = 1;
    private static final int FILTER_STATUS = 2;

    public List<Bridge> findByShardIdWithReadyDependencies(String shardId) {
        Parameters params = Parameters
                .with("shardId", shardId);
        return find("#BRIDGE.findByShardIdWithReadyDependencies", params).list();
    }

    public Bridge findByNameAndCustomerId(String name, String customerId) {
        Parameters params = Parameters
                .with("name", name).and("customerId", customerId);
        return find("#BRIDGE.findByNameAndCustomerId", params).firstResult();
    }

    public Bridge findByIdAndCustomerId(String id, String customerId) {
        Parameters params = Parameters
                .with("id", id).and("customerId", customerId);
        return find("#BRIDGE.findByIdAndCustomerId", params).firstResult();
    }

    public ListResult<Bridge> findByCustomerId(String customerId, QueryResourceInfo queryInfo) {
        Parameters parameters = Parameters.with("customerId", customerId);
        long total;
        List<Bridge> bridges;
        String filterName = queryInfo.getFilterInfo().getFilterName();
        Set<ManagedResourceStatus> filterStatus = queryInfo.getFilterInfo().getFilterStatus();

        int query = 0;
        if (Objects.nonNull(filterName)) {
            query = query | FILTER_NAME;
            parameters = parameters.and("name", filterName + "%");
        }
        if (Objects.nonNull(filterStatus) && !filterStatus.isEmpty()) {
            query = query | FILTER_STATUS;
            parameters = parameters.and("status", filterStatus);
        }

        switch (query) {
            case FILTER_NAME:
                total = find("#BRIDGE.findByCustomerIdFilterByName", parameters).count();
                bridges = find("#BRIDGE.findByCustomerIdFilterByName", parameters).page(queryInfo.getPageNumber(), queryInfo.getPageSize()).list();
                break;
            case FILTER_STATUS:
                total = find("#BRIDGE.findByCustomerIdFilterByStatus", parameters).count();
                bridges = find("#BRIDGE.findByCustomerIdFilterByStatus", parameters).page(queryInfo.getPageNumber(), queryInfo.getPageSize()).list();
                break;
            case (FILTER_NAME + FILTER_STATUS):
                total = find("#BRIDGE.findByCustomerIdFilterByNameAndStatus", parameters).count();
                bridges = find("#BRIDGE.findByCustomerIdFilterByNameAndStatus", parameters).page(queryInfo.getPageNumber(), queryInfo.getPageSize()).list();
                break;
            default:
                total = find("#BRIDGE.findByCustomerId", parameters).count();
                bridges = find("#BRIDGE.findByCustomerId", parameters).page(queryInfo.getPageNumber(), queryInfo.getPageSize()).list();
        }

        return new ListResult<>(bridges, queryInfo.getPageNumber(), total);
    }
}
