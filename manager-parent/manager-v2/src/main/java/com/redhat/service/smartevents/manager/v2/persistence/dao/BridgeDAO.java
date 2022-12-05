package com.redhat.service.smartevents.manager.v2.persistence.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;

import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class BridgeDAO implements ManagedResourceV2DAO<Bridge> {

    @Override
    public Bridge findByIdWithConditions(String id) {
        Parameters params = Parameters
                .with("id", id);
        return find("#BRIDGE_V2.findByIdWithConditions", params).firstResult();
    }

    public Bridge findByNameAndCustomerId(String name, String customerId) {
        Parameters params = Parameters
                .with("name", name).and("customerId", customerId);
        return find("#BRIDGE_V2.findByNameAndCustomerId", params).firstResult();
    }

    public long countByOrganisationId(String organisationId) {
        Parameters params = Parameters
                .with("organisationId", organisationId);
        return count("#BRIDGE_V2.countByOrganisationId", params);
    }
}
