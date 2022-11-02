package com.redhat.service.smartevents.manager.workers.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.manager.persistence.v1.models.Bridge;
import com.redhat.service.smartevents.manager.workers.Work;
import com.redhat.service.smartevents.manager.workers.resources.v1.AbstractWorker;
import com.redhat.service.smartevents.manager.workers.resources.v1.BridgeWorker;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class LegacyBridgeWorker extends AbstractWorker<Bridge> {

    @Inject
    BridgeWorker delegate;

    @Override
    public String getId(Work work) {
        return delegate.getId(work);
    }

    @Override
    public PanacheRepositoryBase<Bridge, String> getDao() {
        return delegate.getDao();
    }

    @Override
    public Bridge createDependencies(Work work, Bridge managedResource) {
        return delegate.createDependencies(work, managedResource);
    }

    @Override
    public Bridge deleteDependencies(Work work, Bridge managedResource) {
        return delegate.deleteDependencies(work, managedResource);
    }

    @Override
    public boolean isProvisioningComplete(Bridge managedResource) {
        return delegate.isProvisioningComplete(managedResource);
    }

    @Override
    public boolean isDeprovisioningComplete(Bridge managedResource) {
        return delegate.isDeprovisioningComplete(managedResource);
    }
}
