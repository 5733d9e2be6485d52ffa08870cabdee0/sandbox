package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class BridgeDAO implements ManagedResourceV2DAO<Bridge> {

    @Override
    public Bridge findByIdWithConditions(String id) {
        Parameters params = Parameters
                .with(Bridge.ID_PARAM, id);
        return find("#BRIDGE_V2.findByIdWithConditions", params).firstResult();
    }

    public Bridge findByIdAndCustomerIdWithConditions(String id, String customerId) {
        Parameters params = Parameters
                .with(Bridge.ID_PARAM, id).and(Bridge.CUSTOMER_ID_PARAM, customerId);
        return find("#BRIDGE.findByIdAndCustomerIdWithConditions", params).firstResult();
    }

    public Bridge findByNameAndCustomerId(String name, String customerId) {
        Parameters params = Parameters
                .with(Bridge.NAME_PARAM, name).and(Bridge.CUSTOMER_ID_PARAM, customerId);
        return find("#BRIDGE_V2.findByNameAndCustomerId", params).firstResult();
    }

    public long countByOrganisationId(String organisationId) {
        Parameters params = Parameters
                .with(Bridge.ORGANISATION_ID_PARAM, organisationId);
        return count("#BRIDGE_V2.countByOrganisationId", params);
    }

    public ListResult<Bridge> findByCustomerId(String customerId, QueryResourceInfo queryInfo) {
        Parameters parameters = Parameters.with(Bridge.CUSTOMER_ID_PARAM, customerId);
        PanacheQuery<Bridge> query = find("#BRIDGE_V2.findByCustomerId", parameters);

        String filterName = queryInfo.getFilterInfo().getFilterName();
        if (Objects.nonNull(filterName)) {
            query.filter("byName", Parameters.with(Bridge.NAME_PARAM, filterName + "%"));
        }

        // As the status of the resource is a view over the conditions, it has to be calculated on the fly. ATM we do it in java, in case there are
        // performance issues we might move the filtering to the database adapting the database schema accordingly.

        List<Bridge> filtered = query.list();
        Set<ManagedResourceStatusV2> filterStatus = queryInfo.getFilterInfo().getFilterStatus();
        if (Objects.nonNull(filterStatus) && !filterStatus.isEmpty()) {
            // Calculate the status of the resource and apply filter.
            filtered = filtered.stream().filter(x -> filterStatus.contains(StatusUtilities.getManagedResourceStatus(x))).collect(Collectors.toList());
        }

        long total = filtered.size();
        int startIndex = queryInfo.getPageNumber() * queryInfo.getPageSize();
        int endIndex = startIndex + queryInfo.getPageSize();

        List<Bridge> bridges = startIndex >= total ? new ArrayList<>() : filtered.subList(startIndex, (int) Math.min(total, endIndex));
        return new ListResult<>(bridges, queryInfo.getPageNumber(), total);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Bridge> findByShardIdToDeployOrDelete(String shardId) {
        EntityManager em = getEntityManager();
        Query q = em.createNamedQuery("BRIDGE_V2.findBridgeIdByShardIdToDeployOrDelete");
        q.setParameter(Bridge.SHARD_ID_PARAM, shardId);
        // Hibernate does not support Lazy Fetches on NativeQueries.
        // Therefore, first get the BridgeId's and then the Bridges using a regular query
        List<String> bridgeIds = (List<String>) q.getResultList();
        Query getBridges = em.createNamedQuery("BRIDGE_V2.findByIdsWithConditions");
        getBridges.setParameter("ids", bridgeIds);
        return (List<Bridge>) getBridges.getResultList();
    }
}
