package com.redhat.developer.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.models.Processor;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

import static java.util.Arrays.asList;

@ApplicationScoped
@Transactional
public class ProcessorDAO implements PanacheRepositoryBase<Processor, String> {

    public Processor findByBridgeIdAndName(String bridgeId, String name) {
        Parameters p = Parameters.with(Processor.NAME_PARAM, name).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        return find("#PROCESSOR.findByBridgeIdAndName", p).firstResultOptional().orElse(null);
    }

    public List<Processor> listProcessorsToDeployOrDelete(String bridgeId) {
        Parameters p = Parameters.with(Processor.BRIDGE_ID_PARAM, bridgeId).and("statuses", asList(BridgeStatus.REQUESTED));
        return find("#PROCESSOR.listByBridgeAndStatus", p).list();
    }
}
