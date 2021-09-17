package com.redhat.service.bridge.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.models.Processor;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

import static java.util.Collections.emptyList;

@ApplicationScoped
@Transactional
public class ProcessorDAO implements PanacheRepositoryBase<Processor, String> {

    private static final String IDS_PARAM = "ids";

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

    private Long countProcessorsOnBridge(Parameters params) {
        TypedQuery<Long> namedQuery = getEntityManager().createNamedQuery("PROCESSOR.countByBridgeIdAndCustomerId", Long.class);
        addParamsToNamedQuery(params, namedQuery);
        return namedQuery.getSingleResult();
    }

    private void addParamsToNamedQuery(Parameters params, TypedQuery<?> namedQuery) {
        params.map().forEach((key, value) -> namedQuery.setParameter(key, value.toString()));
    }

    public ListResult<Processor> findByBridgeIdAndCustomerId(String bridgeId, String customerId, int page, int size) {

        /*
         * Unfortunately we can't rely on Panaches in-built Paging due the fetched join in our query
         * for Processor e.g. join fetch p.bridge. Instead, we simply build a list of ids to fetch and then
         * execute the join fetch as normal. So the workflow here is:
         * 
         * - Count the number of Processors on a bridge. If > 0
         * - Select the ids of the Processors that need to be retrieved based on the page/size requirements
         * - Select the Processors in the list of ids, performing the fetch join of the Bridge
         */

        Parameters p = Parameters.with(Bridge.CUSTOMER_ID_PARAM, customerId).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        Long processorCount = countProcessorsOnBridge(p);
        if (processorCount == 0L) {
            return new ListResult<>(emptyList(), page, processorCount);
        }

        int firstResult = getFirstResult(page, size);
        TypedQuery<String> idsQuery = getEntityManager().createNamedQuery("PROCESSOR.idsByBridgeIdAndCustomerId", String.class);
        addParamsToNamedQuery(p, idsQuery);
        List<String> ids = idsQuery.setMaxResults(size).setFirstResult(firstResult).getResultList();

        List<Processor> processors = list("#PROCESSOR.findByIds", Parameters.with(IDS_PARAM, ids));
        return new ListResult<>(processors, page, processorCount);
    }

    private int getFirstResult(int requestedPage, int requestedPageSize) {
        if (requestedPage <= 0) {
            return 0;
        }

        return requestedPage * requestedPageSize;
    }
}
