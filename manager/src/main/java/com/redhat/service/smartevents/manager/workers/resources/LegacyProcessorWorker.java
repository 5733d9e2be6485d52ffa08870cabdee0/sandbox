package com.redhat.service.smartevents.manager.workers.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.manager.persistence.v1.models.Processor;
import com.redhat.service.smartevents.manager.workers.Work;
import com.redhat.service.smartevents.manager.workers.resources.v1.AbstractWorker;
import com.redhat.service.smartevents.manager.workers.resources.v1.ProcessorWorker;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class LegacyProcessorWorker extends AbstractWorker<Processor> {

    @Inject
    ProcessorWorker delegate;

    @Override
    public String getId(Work work) {
        return delegate.getId(work);
    }

    @Override
    public PanacheRepositoryBase<Processor, String> getDao() {
        return delegate.getDao();
    }

    @Override
    public Processor createDependencies(Work work, Processor managedResource) {
        return delegate.createDependencies(work, managedResource);
    }

    @Override
    public Processor deleteDependencies(Work work, Processor managedResource) {
        return delegate.deleteDependencies(work, managedResource);
    }

    @Override
    public boolean isProvisioningComplete(Processor managedResource) {
        return delegate.isProvisioningComplete(managedResource);
    }

    @Override
    public boolean isDeprovisioningComplete(Processor managedResource) {
        return delegate.isDeprovisioningComplete(managedResource);
    }

}
