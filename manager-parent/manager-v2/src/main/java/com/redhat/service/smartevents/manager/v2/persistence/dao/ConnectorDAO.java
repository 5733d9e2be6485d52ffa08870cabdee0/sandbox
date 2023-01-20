package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;

public abstract class ConnectorDAO implements ManagedResourceV2DAO<Connector> {

    private ConnectorType type;

    public ConnectorDAO(ConnectorType type) {
        this.type = type;
    }

    public Connector findByIdWithConditions(String id) {
        Parameters params = Parameters
                .with(Connector.ID_PARAM, id)
                .and(Connector.TYPE_PARAM, type);
        return find("#CONNECTOR_V2.findByIdWithConditions", params).firstResult();
    }

    public Connector findByBridgeIdAndName(String bridgeId, String name) {
        Parameters params = Parameters
                .with(Connector.NAME_PARAM, name)
                .and(Connector.BRIDGE_ID_PARAM, bridgeId)
                .and(Connector.TYPE_PARAM, type);
        return find("#CONNECTOR_V2.findByBridgeIdAndName", params).firstResult();
    }

    public Connector findByIdBridgeIdAndCustomerId(String bridgeId, String connectorId, String customerId) {
        Parameters params = Parameters.with(Connector.ID_PARAM, connectorId)
                .and(Connector.BRIDGE_ID_PARAM, bridgeId)
                .and(Bridge.CUSTOMER_ID_PARAM, customerId)
                .and(Connector.TYPE_PARAM, type);
        return find("#CONNECTOR_V2.findByIdBridgeIdAndCustomerId", params).firstResult();
    }

    public ListResult<Connector> findByBridgeIdAndCustomerId(String bridgeId, String customerId, QueryResourceInfo queryInfo) {
        Parameters parameters = Parameters.with(Connector.BRIDGE_ID_PARAM, bridgeId).and(Bridge.CUSTOMER_ID_PARAM, customerId);
        PanacheQuery<Connector> query = find("#CONNECTOR_V2.findByBridgeIdAndCustomerId", parameters);

        String filterName = queryInfo.getFilterInfo().getFilterName();
        if (Objects.nonNull(filterName)) {
            query.filter("byName", Parameters.with(Connector.NAME_PARAM, filterName + "%"));
        }

        // As the status of the resource is a view over the conditions, it has to be calculated on the fly. ATM we do it in java, in case there are
        // performance issues we might move the filtering to the database adapting the database schema accordingly.

        List<Connector> filtered = query.list();
        Set<ManagedResourceStatusV2> filterStatus = queryInfo.getFilterInfo().getFilterStatus();
        if (Objects.nonNull(filterStatus) && !filterStatus.isEmpty()) {
            // Calculate the status of the resource and apply filter.
            filtered = filtered.stream().filter(x -> filterStatus.contains(StatusUtilities.getManagedResourceStatus(x))).collect(Collectors.toList());
        }

        long total = filtered.size();
        int startIndex = queryInfo.getPageNumber() * queryInfo.getPageSize();
        int endIndex = startIndex + queryInfo.getPageSize();

        List<Connector> connectors = startIndex >= total ? new ArrayList<>() : filtered.subList(startIndex, (int) Math.min(total, endIndex));
        return new ListResult<>(connectors, queryInfo.getPageNumber(), total);
    }

    public long countByBridgeIdAndCustomerId(String bridgeId, String customerId) {
        TypedQuery<Long> namedQuery = getEntityManager().createNamedQuery("CONNECTOR_V2.countByBridgeIdAndCustomerId", Long.class);
        namedQuery.setParameter(Connector.BRIDGE_ID_PARAM, bridgeId);
        namedQuery.setParameter(Bridge.CUSTOMER_ID_PARAM, customerId);
        namedQuery.setParameter(Connector.TYPE_PARAM, type);
        return namedQuery.getSingleResult();
    }

    @Override
    public List<Connector> findByShardIdToDeployOrDelete(String shardId) {
        EntityManager em = getEntityManager();
        Query q = em.createNamedQuery("CONNECTOR_V2.findConnectorIdByShardIdToDeployOrDelete");
        q.setParameter(Bridge.SHARD_ID_PARAM, shardId);
        // Native queries don't accept an enum as parameter. It has to be converted to string.
        q.setParameter(Connector.TYPE_PARAM, type.name());
        // Hibernate does not support Lazy Fetches on NativeQueries.
        // Therefore, first get the ConnectorIds's and then the Connectors using a regular query
        List<String> connectorIds = (List<String>) q.getResultList();
        Query getConnectors = em.createNamedQuery("CONNECTOR_V2.findByIdsWithBridgeAndConditions");
        getConnectors.setParameter("ids", connectorIds);
        return (List<Connector>) getConnectors.getResultList();
    }
}
