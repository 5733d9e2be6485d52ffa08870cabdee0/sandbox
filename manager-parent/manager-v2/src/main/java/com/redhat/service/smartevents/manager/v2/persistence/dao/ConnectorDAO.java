package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

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
        Parameters params = Parameters.with(Processor.NAME_PARAM, name)
                .and(Connector.BRIDGE_ID_PARAM, bridgeId)
                .and(Connector.TYPE_PARAM, type);
        return find("#CONNECTOR_V2.findByBridgeIdAndName", params).firstResult();
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
