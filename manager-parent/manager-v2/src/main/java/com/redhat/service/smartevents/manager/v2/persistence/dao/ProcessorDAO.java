package com.redhat.service.smartevents.manager.v2.persistence.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class ProcessorDAO implements ManagedResourceV2DAO<Processor> {

    @Override
    public Processor findByIdWithConditions(String id) {
        Parameters params = Parameters
                .with("id", id);
        return find("#PROCESSOR_V2.findByIdWithConditions", params).firstResult();
    }

}
