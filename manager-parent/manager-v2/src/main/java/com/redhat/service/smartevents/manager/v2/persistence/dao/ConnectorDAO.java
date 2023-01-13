package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;

import io.quarkus.panache.common.Parameters;

public abstract class ConnectorDAO implements ManagedResourceV2DAO<Connector> {

    private ConnectorType type;

    public ConnectorDAO(ConnectorType type) {
        this.type = type;
    }

    public Connector findByIdWithConditions(String id) {
        Parameters params = Parameters.with("id", id).and("type", type);
        return find("#CONNECTOR_V2.findByIdWithConditions", params).firstResult();
    }

    @Override
    public List<Connector> findByShardIdToDeployOrDelete(String shardId) {
        EntityManager em = getEntityManager();
        Query q = em.createNamedQuery("CONNECTOR_V2.findConnectorIdByShardIdToDeployOrDelete");
        q.setParameter("shardId", shardId);
        // Native queries don't accept an enum as parameter. It has to be converted to string.
        q.setParameter("type", type.name());
        // Hibernate does not support Lazy Fetches on NativeQueries.
        // Therefore, first get the ConnectorIds's and then the Connectors using a regular query
        List<String> connectorIds = (List<String>) q.getResultList();
        Query getConnectors = em.createNamedQuery("CONNECTOR_V2.findByIdsWithBridgeAndConditions");
        getConnectors.setParameter("ids", connectorIds);
        return (List<Connector>) getConnectors.getResultList();
    }
}
