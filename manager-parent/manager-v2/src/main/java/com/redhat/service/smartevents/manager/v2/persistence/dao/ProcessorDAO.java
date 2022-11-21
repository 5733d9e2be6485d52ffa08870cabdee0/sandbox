package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@Transactional
@ApplicationScoped
public class ProcessorDAO implements PanacheRepositoryBase<Processor, String> {

    public Processor findByBridgeIdAndName(String bridgeId, String name) {
        Parameters p = Parameters.with(Processor.NAME_PARAM, name).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        return singleResultFromList(find("#PROCESSOR_V2.findByBridgeIdAndName", p));
    }

    private Processor singleResultFromList(PanacheQuery<Processor> find) {
        List<Processor> processors = find.list();
        if (processors.size() > 1) {
            throw new IllegalStateException("Multiple Entities returned from a Query that should only return a single Entity");
        }
        return processors.size() == 1 ? processors.get(0) : null;
    }

    public long countByBridgeIdAndCustomerId(String bridgeId, String customerId) {
        TypedQuery<Long> namedQuery = getEntityManager().createNamedQuery("PROCESSOR_V2.countByBridgeIdAndCustomerId", Long.class);
        namedQuery.setParameter(Processor.BRIDGE_ID_PARAM, bridgeId);
        namedQuery.setParameter(Bridge.CUSTOMER_ID_PARAM, customerId);
        return namedQuery.getSingleResult();
    }

}
