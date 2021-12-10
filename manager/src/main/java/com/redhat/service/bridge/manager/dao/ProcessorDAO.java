package com.redhat.service.bridge.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import com.redhat.service.bridge.infra.api.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.models.QueryInfo;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

import static java.util.Collections.emptyList;

@ApplicationScoped
@Transactional
public class ProcessorDAO implements PanacheRepositoryBase<Processor, String> {

    /*
     * NOTE: the Processor queries that use a left join on the filters **MUST** be wrapped by the method `removeDuplicates`!
     * see https://developer.jboss.org/docs/DOC-15782#
     * jive_content_id_Hibernate_does_not_return_distinct_results_for_a_query_with_outer_join_fetching_enabled_for_a_collection_even_if_I_use_the_distinct_keyword
     */

    private static final String IDS_PARAM = "ids";

    public Processor findByBridgeIdAndName(String bridgeId, String name) {
        Parameters p = Parameters.with(Processor.NAME_PARAM, name).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        return singleResultFromList(find("#PROCESSOR.findByBridgeIdAndName", p));
    }

    /*
     * For queries where we need to fetch join associations, this works around the fact that Hibernate has to
     * apply pagination in-memory _if_ we rely on Panaches .firstResult() or firstResultOptional() methods. This
     * manifests as "HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!" in the log
     * 
     * This performs the query as if we expect a list result, but then converts the list into a single result
     * response: either the entity if the list has a single result, or null if not.
     *
     * More than 1 entity in the list throws an IllegalStateException as it's not something that we expect to happen
     * 
     */
    private Processor singleResultFromList(PanacheQuery<Processor> find) {
        List<Processor> processors = find.list();
        if (processors.size() > 1) {
            throw new IllegalStateException("Multiple Entities returned from a Query that should only return a single Entity");
        }
        return processors.size() == 1 ? processors.get(0) : null;
    }

    public Processor findByIdBridgeIdAndCustomerId(String id, String bridgeId, String customerId) {

        Parameters p = Parameters.with(Processor.ID_PARAM, id)
                .and(Processor.BRIDGE_ID_PARAM, bridgeId)
                .and(Bridge.CUSTOMER_ID_PARAM, customerId);

        return singleResultFromList(find("#PROCESSOR.findByIdBridgeIdAndCustomerId", p));
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

    public ListResult<Processor> findByBridgeIdAndCustomerId(String bridgeId, String customerId, QueryInfo queryInfo) {

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
            return new ListResult<>(emptyList(), queryInfo.getPageNumber(), processorCount);
        }

        int firstResult = getFirstResult(queryInfo.getPageNumber(), queryInfo.getPageSize());
        TypedQuery<String> idsQuery = getEntityManager().createNamedQuery("PROCESSOR.idsByBridgeIdAndCustomerId", String.class);
        addParamsToNamedQuery(p, idsQuery);
        List<String> ids = idsQuery.setMaxResults(queryInfo.getPageSize()).setFirstResult(firstResult).getResultList();

        List<Processor> processors = getEntityManager().createNamedQuery("PROCESSOR.findByIds", Processor.class).setParameter(IDS_PARAM, ids).getResultList();
        return new ListResult<>(processors, queryInfo.getPageNumber(), processorCount);
    }

    public Long countByBridgeIdAndCustomerId(String bridgeId, String customerId) {
        Parameters p = Parameters.with(Bridge.CUSTOMER_ID_PARAM, customerId).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        return countProcessorsOnBridge(p);
    }

    private int getFirstResult(int requestedPage, int requestedPageSize) {
        if (requestedPage <= 0) {
            return 0;
        }

        return requestedPage * requestedPageSize;
    }
}
