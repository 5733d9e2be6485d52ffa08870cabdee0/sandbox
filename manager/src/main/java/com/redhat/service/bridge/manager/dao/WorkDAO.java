package com.redhat.service.bridge.manager.dao;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.bridge.manager.models.ManagedResource;
import com.redhat.service.bridge.manager.models.Work;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@Transactional
@ApplicationScoped
public class WorkDAO implements PanacheRepositoryBase<Work, String> {

    public Work findByManagedResource(ManagedResource managedResource) {
        return find("#Work.findByManagedResourceId", Parameters.with("managedResourceId", managedResource.getId())).firstResult();
    }

    public Stream<Work> findByWorkerId(String workerId) {
        return stream("#Work.findByWorkerId", Parameters.with("workerId", workerId));
    }

    public int rebalanceWork(String workerId, ZonedDateTime lastUpdated) {
        return update("#Work.updateWorkerId", Parameters.with("workerId", workerId).and("age", lastUpdated));
    }
}
