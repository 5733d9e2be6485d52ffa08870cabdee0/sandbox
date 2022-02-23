package com.redhat.service.bridge.manager.dao;

import java.time.ZonedDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.bridge.manager.models.PreparingWorker;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class PreparingWorkerDAO implements PanacheRepositoryBase<PreparingWorker, String> {

    public List<PreparingWorker> findByEntityId(String entityId) {
        Parameters params = Parameters
                .with("entityId", entityId);
        return find("#PREPARINGWORKER.findByEntityId", params).list();
    }

    public List<PreparingWorker> findByWorkerIdAndStatusAndType(String workerId, String status, String type) {
        Parameters params = Parameters
                .with("workerId", workerId)
                .and("status", status)
                .and("type", type);
        return find("#PREPARINGWORKER.findByWorkerIdAndStatusAndType", params).list();
    }

    public List<PreparingWorker> findByAgeAndStatusAndType(ZonedDateTime age, String status, String type) {
        Parameters params = Parameters
                .with("age", age)
                .and("status", status)
                .and("type", type);
        return find("#PREPARINGWORKER.findByAgeAndStatusAndType", params).list();
    }

}
