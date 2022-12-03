package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class BridgeDAO implements PanacheRepositoryBase<Bridge, String> {

    public Bridge findByIdWithConditions(String id) {
        Parameters params = Parameters
                .with("id", id);
        return find("#BRIDGE_V2.findByIdWithConditions", params).firstResult();
    }

    public Bridge findByNameAndCustomerId(String name, String customerId) {
        Parameters params = Parameters
                .with("name", name).and("customerId", customerId);
        return find("#BRIDGE_V2.findByNameAndCustomerId", params).firstResult();
    }

    public long countByOrganisationId(String organisationId) {
        Parameters params = Parameters
                .with("organisationId", organisationId);
        return count("#BRIDGE_V2.countByOrganisationId", params);
    }

    public ListResult<Bridge> findByCustomerId(String customerId, QueryResourceInfo queryInfo) {
        Parameters parameters = Parameters.with("customerId", customerId);
        PanacheQuery<Bridge> query = find("#BRIDGE_V2.findByCustomerId", parameters);

        String filterName = queryInfo.getFilterInfo().getFilterName();
        if (Objects.nonNull(filterName)) {
            query.filter("byName", Parameters.with("name", filterName + "%"));
        }

        // As the status of the resource is a view over the conditions, it has to be calculated on the fly. ATM we do it in java, in case there are
        // performance issues we might move the filtering to the database adapting the database schema accordingly.

        List<Bridge> filtered = query.list();
        Set<ManagedResourceStatus> filterStatus = queryInfo.getFilterInfo().getFilterStatus();
        if (Objects.nonNull(filterStatus) && !filterStatus.isEmpty()) {
            // Calculate the status of the resource and apply filter.
            filtered = filtered.stream().filter(x -> filterStatus.contains(StatusUtilities.getManagedResourceStatus(x))).collect(Collectors.toList());
        }

        long total = filtered.size();
        int startIndex = queryInfo.getPageNumber() * queryInfo.getPageSize();
        int endIndex = startIndex + queryInfo.getPageSize();

        List<Bridge> bridges = startIndex >= total ? new ArrayList<>() : filtered.subList(startIndex, (int) Math.min(total, endIndex));
        return new ListResult<>(bridges, queryInfo.getPageNumber(), total);
    }
}
