package com.redhat.service.bridge.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Processor;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class ProcessorDAO implements PanacheRepositoryBase<Processor, String> {

    public Processor findByBridgeIdAndName(String bridgeId, String name) {
        Parameters p = Parameters.with(Processor.NAME_PARAM, name).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        return find("#PROCESSOR.findByBridgeIdAndName", p).firstResultOptional().orElse(null);
    }

    public Processor findByIdBridgeIdAndCustomerId(String id, String bridgeId, String customerId) {

        Parameters p = Parameters.with(Processor.ID_PARAM, id)
                .and(Processor.BRIDGE_ID_PARAM, bridgeId)
                .and(Bridge.CUSTOMER_ID_PARAM, customerId);

        return find("#PROCESSOR.findByIdBridgeIdAndCustomerId", p).firstResultOptional().orElse(null);
    }

    public List<Processor> findByStatuses(List<BridgeStatus> statuses) {
        Parameters p = Parameters.with("statuses", statuses);
        return find("#PROCESSOR.findByStatus", p).list();
    }
}
