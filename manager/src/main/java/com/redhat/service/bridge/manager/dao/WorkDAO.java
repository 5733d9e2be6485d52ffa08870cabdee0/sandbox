package com.redhat.service.bridge.manager.dao;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import com.redhat.service.bridge.manager.models.ManagedEntity;
import com.redhat.service.bridge.manager.workers.Work;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

/*
 * Incredibly basic DAO for managing items in the work queue
 */
@Transactional
public class WorkDAO implements PanacheRepositoryBase<Work, String> {

    public Work findByEntity(ManagedEntity managedEntity) {
        return find("#Work.findByEntityId", Parameters.with("entityId", managedEntity.getId())).firstResult();
    }

    public Stream<Work> findByWorkerId(String workerId) {
        return stream("#Work.findByWorkerId", Parameters.with("workerId", workerId));
    }

    public int rebalanceWork(String workerId, ZonedDateTime lastUpdated) {
        return update("#Work.updateWorkerId", Parameters.with("workerId", workerId).and("age", lastUpdated));
    }
}
