package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;

@Transactional
@ApplicationScoped
public class ProcessorDAO implements ManagedResourceV2DAO<Processor> {

    @Override
    public Processor findByIdWithConditions(String id) {
        Parameters params = Parameters
                .with("id", id);
        return find("#PROCESSOR_V2.findByIdWithConditions", params).firstResult();
    }

    public Processor findByBridgeIdAndName(String bridgeId, String name) {
        Parameters params = Parameters.with(Processor.NAME_PARAM, name).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        return find("#PROCESSOR_V2.findByBridgeIdAndName", params).firstResult();
    }

    public Processor findByIdBridgeIdAndCustomerId(String bridgeId, String processorId, String customerId) {
        Parameters params = Parameters.with(Processor.ID_PARAM, processorId)
                .and(Processor.BRIDGE_ID_PARAM, bridgeId)
                .and(Bridge.CUSTOMER_ID_PARAM, customerId);
        return find("#PROCESSOR_V2.findByIdBridgeIdAndCustomerId", params).firstResult();
    }

    public ListResult<Processor> findByBridgeIdAndCustomerId(String bridgeId, String customerId, QueryResourceInfo queryInfo) {
        Parameters parameters = Parameters.with("bridgeId", bridgeId).and("customerId", customerId);
        PanacheQuery<Processor> query = find("#PROCESSOR_V2.findByBridgeIdAndCustomerId", parameters);

        String filterName = queryInfo.getFilterInfo().getFilterName();
        if (Objects.nonNull(filterName)) {
            query.filter("byName", Parameters.with("name", filterName + "%"));
        }

        // As the status of the resource is a view over the conditions, it has to be calculated on the fly. ATM we do it in java, in case there are
        // performance issues we might move the filtering to the database adapting the database schema accordingly.

        List<Processor> filtered = query.list();
        Set<ManagedResourceStatus> filterStatus = queryInfo.getFilterInfo().getFilterStatus();
        if (Objects.nonNull(filterStatus) && !filterStatus.isEmpty()) {
            // Calculate the status of the resource and apply filter.
            filtered = filtered.stream().filter(x -> filterStatus.contains(StatusUtilities.getManagedResourceStatus(x))).collect(Collectors.toList());
        }

        long total = filtered.size();
        int startIndex = queryInfo.getPageNumber() * queryInfo.getPageSize();
        int endIndex = startIndex + queryInfo.getPageSize();

        List<Processor> processors = startIndex >= total ? new ArrayList<>() : filtered.subList(startIndex, (int) Math.min(total, endIndex));
        return new ListResult<>(processors, queryInfo.getPageNumber(), total);
    }

    public long countByBridgeIdAndCustomerId(String bridgeId, String customerId) {
        TypedQuery<Long> namedQuery = getEntityManager().createNamedQuery("PROCESSOR_V2.countByBridgeIdAndCustomerId", Long.class);
        namedQuery.setParameter(Processor.BRIDGE_ID_PARAM, bridgeId);
        namedQuery.setParameter(Bridge.CUSTOMER_ID_PARAM, customerId);
        return namedQuery.getSingleResult();
    }
}
