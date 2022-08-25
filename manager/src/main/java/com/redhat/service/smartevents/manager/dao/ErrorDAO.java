package com.redhat.service.smartevents.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.manager.models.ProcessingError;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class ErrorDAO implements PanacheRepositoryBase<ProcessingError, Long> {

    public ListResult<ProcessingError> findByBridgeIdOrdered(String bridgeId, QueryResourceInfo queryInfo) {
        PanacheQuery<ProcessingError> query = find("#PROCESSING_ERROR.findByBridgeIdOrdered", Parameters.with("bridgeId", bridgeId));
        long total = query.count();
        List<ProcessingError> processingErrors = query.page(queryInfo.getPageNumber(), queryInfo.getPageSize()).list();
        return new ListResult<>(processingErrors, queryInfo.getPageNumber(), total);
    }
}
