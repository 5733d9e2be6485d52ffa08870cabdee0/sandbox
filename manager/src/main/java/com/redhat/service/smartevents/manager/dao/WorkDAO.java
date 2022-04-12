package com.redhat.service.smartevents.manager.dao;

import java.time.ZonedDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.models.ManagedResource;
import com.redhat.service.smartevents.manager.models.Work;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@Transactional
@ApplicationScoped
public class WorkDAO implements PanacheRepositoryBase<Work, String> {

    public Work findByManagedResource(ManagedResource managedResource) {
        return find("#Work.findByManagedResourceId", Parameters.with("managedResourceId", managedResource.getId())).firstResult();
    }

    public List<Work> findByWorkerId(String workerId) {
        return list("#Work.findByWorkerId", Parameters.with("workerId", workerId));
    }

    public int reconnectDroppedWorkers(String workerId, ZonedDateTime lastUpdated) {
        return update("#Work.reconnectDroppedWorkers",
                Parameters.with("workerId", workerId)
                        .and("now", ZonedDateTime.now())
                        .and("age", lastUpdated));
    }
}
