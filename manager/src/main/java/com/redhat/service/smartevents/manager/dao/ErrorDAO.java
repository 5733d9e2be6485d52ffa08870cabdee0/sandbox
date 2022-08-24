package com.redhat.service.smartevents.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.manager.models.Error;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class ErrorDAO implements PanacheRepositoryBase<Error, Long> {

    public ListResult<Error> findByBridgeIdOrdered(String bridgeId, QueryResourceInfo queryInfo) {
        PanacheQuery<Error> query = find("#ERROR.findByBridgeIdOrdered", Parameters.with("bridgeId", bridgeId));
        long total = query.count();
        List<Error> errors = query.page(queryInfo.getPageNumber(), queryInfo.getPageSize()).list();
        return new ListResult<>(errors, queryInfo.getPageNumber(), total);
    }
}
