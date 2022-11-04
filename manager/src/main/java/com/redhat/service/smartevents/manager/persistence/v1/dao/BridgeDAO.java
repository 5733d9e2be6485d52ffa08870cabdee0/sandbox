package com.redhat.service.smartevents.manager.persistence.v1.dao;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.persistence.v1.models.Bridge;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class BridgeDAO implements PanacheRepositoryBase<Bridge, String> {

    public List<Bridge> findByShardIdToDeployOrDelete(String shardId) {
        Parameters params = Parameters
                .with("shardId", shardId);
        return find("#BRIDGE.findByShardIdToDeployOrDelete", params).list();
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

    public long countByOrganisationId(String organisationId) {
        Parameters params = Parameters
                .with("organisationId", organisationId);
        return count("#BRIDGE.countByOrganisationId", params);
    }

    public ListResult<Bridge> findByCustomerId(String customerId, QueryResourceInfo queryInfo) {
        Parameters parameters = Parameters.with("customerId", customerId);
        PanacheQuery<Bridge> query = find("#BRIDGE.findByCustomerId", parameters);

        String filterName = queryInfo.getFilterInfo().getFilterName();
        Set<ManagedResourceStatus> filterStatus = queryInfo.getFilterInfo().getFilterStatus();
        if (Objects.nonNull(filterName)) {
            query.filter("byName", Parameters.with("name", filterName + "%"));
        }
        if (Objects.nonNull(filterStatus) && !filterStatus.isEmpty()) {
            query.filter("byStatus", Parameters.with("status", filterStatus));
        }

        long total = query.count();
        List<Bridge> bridges = query.page(queryInfo.getPageNumber(), queryInfo.getPageSize()).list();

        return new ListResult<>(bridges, queryInfo.getPageNumber(), total);
    }
}
