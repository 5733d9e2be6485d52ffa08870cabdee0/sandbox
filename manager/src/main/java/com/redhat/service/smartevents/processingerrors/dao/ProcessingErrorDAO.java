package com.redhat.service.smartevents.processingerrors.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.processingerrors.models.ProcessingError;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

import static com.redhat.service.smartevents.processingerrors.models.ProcessingError.FIND_BY_BRIDGE_ID_ORDERED_QUERY;

@ApplicationScoped
@Transactional
public class ProcessingErrorDAO implements PanacheRepositoryBase<ProcessingError, Long> {

    private static final String CLEANUP_PROCEDURE = "cleanup_processing_error";
    private static final String CLEANUP_QUERY = String.format("CALL %s(?)", CLEANUP_PROCEDURE);

    public ListResult<ProcessingError> findByBridgeIdOrdered(String bridgeId, QueryResourceInfo queryInfo) {
        PanacheQuery<ProcessingError> query = find("#" + FIND_BY_BRIDGE_ID_ORDERED_QUERY, Parameters.with("bridgeId", bridgeId));
        long total = query.count();
        List<ProcessingError> processingErrors = query.page(queryInfo.getPageNumber(), queryInfo.getPageSize()).list();
        return new ListResult<>(processingErrors, queryInfo.getPageNumber(), total);
    }

    public void cleanup(int maxErrorsPerBridge) {
        getEntityManager()
                .createNativeQuery(CLEANUP_QUERY)
                .setParameter(1, maxErrorsPerBridge)
                .executeUpdate();
    }
}
