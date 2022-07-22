package com.redhat.service.smartevents.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.models.WorkError;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@Transactional
@ApplicationScoped
public class WorkErrorDAO implements PanacheRepositoryBase<WorkError, String> {

    public List<WorkError> findByManagedResourceId(String managedResourceId) {
        Parameters params = Parameters.with("managedResourceId", managedResourceId);
        return find("#WorkError.findByManagedResourceId", params).list();
    }

    public void deleteByManagedResourceId(String managedResourceId) {
        Parameters params = Parameters.with("managedResourceId", managedResourceId);
        delete("#WorkError.deleteByManagedResourceId", params);
    }

}
