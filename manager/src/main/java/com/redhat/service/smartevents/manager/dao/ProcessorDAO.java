package com.redhat.service.smartevents.manager.dao;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryProcessorResourceInfo;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;

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
    private static final Set<ProcessorType> USER_VISIBLE_PROCESSOR_TYPES = Set.of(ProcessorType.SOURCE, ProcessorType.SINK);
    private static final Set<ProcessorType> HIDDEN_PROCESSOR_TYPES = Set.of(ProcessorType.ERROR_HANDLER);
    private static final String BY_TYPE_FILTER_NAME = "byType";
    private static final String BY_TYPE_FILTER_PARAM = "ptype";

    private static class ProcessorResults {

        List<String> ids;

        long total;

        ProcessorResults(List<String> ids, long total) {
            this.ids = ids;
            this.total = total;
        }
    }

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

    public Processor findByIdBridgeIdAndCustomerId(String bridgeId, String processorId, String customerId) {

        Parameters p = Parameters.with(Processor.ID_PARAM, processorId)
                .and(Processor.BRIDGE_ID_PARAM, bridgeId)
                .and(Bridge.CUSTOMER_ID_PARAM, customerId);

        return singleResultFromList(find("#PROCESSOR.findByIdBridgeIdAndCustomerId", p));
    }

    public List<Processor> findByShardIdWithReadyDependencies(String shardId) {
        Parameters p = Parameters
                .with("shardId", shardId);
        return find("#PROCESSOR.findByShardIdWithReadyDependencies", p).list();
    }

    private Long countProcessorsOnBridge(Parameters params) {
        TypedQuery<Long> namedQuery = getEntityManager().createNamedQuery("PROCESSOR.countByBridgeIdAndCustomerId", Long.class);
        addParamsToNamedQuery(params, namedQuery);
        return namedQuery.getSingleResult();
    }

    private void addParamsToNamedQuery(Parameters params, TypedQuery<?> namedQuery) {
        params.map().forEach((key, value) -> namedQuery.setParameter(key, value.toString()));
    }

    public ListResult<Processor> findUserVisibleByBridgeIdAndCustomerId(String bridgeId, String customerId, QueryProcessorResourceInfo queryInfo) {
        return findByBridgeIdAndCustomerId(bridgeId, customerId, queryInfo, USER_VISIBLE_PROCESSOR_TYPES);
    }

    public ListResult<Processor> findHiddenByBridgeIdAndCustomerId(String bridgeId, String customerId, QueryProcessorResourceInfo queryInfo) {
        return findByBridgeIdAndCustomerId(bridgeId, customerId, queryInfo, HIDDEN_PROCESSOR_TYPES);
    }

    public ListResult<Processor> findByBridgeIdAndCustomerId(String bridgeId, String customerId, QueryProcessorResourceInfo queryInfo) {
        return findByBridgeIdAndCustomerId(bridgeId, customerId, queryInfo, null);
    }

    private ListResult<Processor> findByBridgeIdAndCustomerId(String bridgeId, String customerId, QueryProcessorResourceInfo queryInfo, Set<ProcessorType> restrictTypes) {

        /*
         * Unfortunately we can't rely on Panaches in-built Paging due the fetched join in our query
         * for Processor e.g. join fetch p.bridge. Instead, we simply build a list of ids to fetch and then
         * execute the join fetch as normal. So the workflow here is:
         *
         * - Count the number of Processors on a bridge. If > 0
         * - Select the ids of the Processors that need to be retrieved based on the page/size requirements
         * - Select the Processors in the list of ids, performing the fetch join of the Bridge
         */

        // We don't consider filtering here; so this could be short-cut more but the additional code doesn't really make it worthwhile
        Parameters p = Parameters.with(Bridge.CUSTOMER_ID_PARAM, customerId).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        Long processorCount = countProcessorsOnBridge(p);
        if (processorCount == 0L) {
            return new ListResult<>(emptyList(), queryInfo.getPageNumber(), processorCount);
        }

        ProcessorResults results = getProcessorIds(customerId, bridgeId, queryInfo, restrictTypes);
        // The Query #PROCESSOR.findByIds must also order the result set otherwise ordering achieved by getProcessorIds() is lost.
        List<Processor> processors = find("#PROCESSOR.findByIds", Parameters.with(IDS_PARAM, results.ids)).list();
        return new ListResult<>(processors, queryInfo.getPageNumber(), results.total);
    }

    private ProcessorResults getProcessorIds(String customerId, String bridgeId, QueryProcessorResourceInfo queryInfo, Set<ProcessorType> restrictTypes) {
        Parameters parameters = Parameters.with("customerId", customerId).and("bridgeId", bridgeId);
        PanacheQuery<Processor> query = find("#PROCESSOR.findByBridgeIdAndCustomerIdNoFilter", parameters);

        // filter by name
        String filterName = queryInfo.getFilterInfo().getFilterName();
        if (Objects.nonNull(filterName)) {
            query.filter("byName", Parameters.with("name", filterName + "%"));
        }

        // filter by status
        Set<ManagedResourceStatus> filterStatus = queryInfo.getFilterInfo().getFilterStatus();
        if (Objects.nonNull(filterStatus) && !filterStatus.isEmpty()) {
            query.filter("byStatus", Parameters.with("status", filterStatus));
        }

        // filter by type, considering onlyUserVisible flag
        ProcessorType filterType = queryInfo.getFilterInfo().getFilterType();
        if (restrictTypes != null) {
            if (Objects.isNull(filterType)) {
                query.filter(BY_TYPE_FILTER_NAME, Parameters.with(BY_TYPE_FILTER_PARAM, restrictTypes));
            } else {
                if (restrictTypes.contains(filterType)) {
                    query.filter(BY_TYPE_FILTER_NAME, Parameters.with(BY_TYPE_FILTER_PARAM, Set.of(filterType)));
                } else {
                    return new ProcessorResults(emptyList(), 0);
                }
            }
        } else {
            if (Objects.nonNull(filterType)) {
                query.filter(BY_TYPE_FILTER_NAME, Parameters.with(BY_TYPE_FILTER_PARAM, Set.of(filterType)));
            }
        }

        long total = query.count();
        List<Processor> processors = query.page(queryInfo.getPageNumber(), queryInfo.getPageSize()).list();
        List<String> ids = processors.stream().map(Processor::getId).collect(Collectors.toList());

        return new ProcessorResults(ids, total);
    }

    public Long countByBridgeIdAndCustomerId(String bridgeId, String customerId) {
        Parameters p = Parameters.with(Bridge.CUSTOMER_ID_PARAM, customerId).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        return countProcessorsOnBridge(p);
    }

    public Long countByBridgeId(String bridgeId) {
        TypedQuery<Long> namedQuery = getEntityManager().createNamedQuery("PROCESSOR.countByBridgeId", Long.class);
        Parameters params = Parameters
                .with(Processor.BRIDGE_ID_PARAM, bridgeId);
        addParamsToNamedQuery(params, namedQuery);
        return namedQuery.getSingleResult();
    }
}
