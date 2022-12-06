package com.redhat.service.smartevents.manager.v2.persistence.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@Transactional
@ApplicationScoped
public class ProcessorDAO implements PanacheRepositoryBase<Processor, String> {

    public Processor findByBridgeIdAndName(String bridgeId, String name) {
        Parameters params = Parameters.with(Processor.NAME_PARAM, name).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        return find("#PROCESSOR_V2.findByBridgeIdAndName", params).firstResult();
    }

    public long countByBridgeIdAndCustomerId(String bridgeId, String customerId) {
        TypedQuery<Long> namedQuery = getEntityManager().createNamedQuery("PROCESSOR_V2.countByBridgeIdAndCustomerId", Long.class);
        namedQuery.setParameter(Processor.BRIDGE_ID_PARAM, bridgeId);
        namedQuery.setParameter(Bridge.CUSTOMER_ID_PARAM, customerId);
        return namedQuery.getSingleResult();
    }

}
