package com.redhat.service.bridge.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.models.QueryInfo;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class BridgeDAO implements PanacheRepositoryBase<Bridge, String> {

    public List<Bridge> findByStatuses(List<BridgeStatus> statuses) {
        Parameters params = Parameters
                .with("statuses", statuses);
        return find("#BRIDGE.findByStatuses", params).list();
    }

    public Bridge findByNameAndCustomerId(String name, String customerId) {
        Parameters params = Parameters
                .with("name", name).and("customerId", customerId);
        return find("#BRIDGE.findByNameAndCustomerId", params).firstResult();
    }

    public Bridge findByIdAndCustomerId(String id, String customerId) {
        Parameters params = Parameters
                .with("id", id).and("customerId", customerId);
        return find("#BRIDGE.findByIdAndCustomerId", params).firstResult();
    }

    public ListResult<Bridge> findByCustomerId(String customerId, QueryInfo queryInfo) {
        Parameters parameters = Parameters.with("customerId", customerId);
        long total = find("#BRIDGE.findByCustomerId", parameters).count();
        List<Bridge> bridges = find("#BRIDGE.findByCustomerId", parameters).page(queryInfo.getPageNumber(), queryInfo.getPageSize()).list();
        return new ListResult<>(bridges, queryInfo.getPageNumber(), total);
    }
}
