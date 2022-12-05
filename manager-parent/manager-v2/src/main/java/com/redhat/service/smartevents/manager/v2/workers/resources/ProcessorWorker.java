package com.redhat.service.smartevents.manager.v2.workers.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class ProcessorWorker extends AbstractWorker<Processor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorWorker.class);

    @Inject
    ProcessorDAO processorDAO;

    @Override
    public PanacheRepositoryBase<Processor, String> getDao() {
        return processorDAO;
    }

    @Override
    public String getId(Work work) {
        // The ID of the ManagedResource to process is stored directly in the JobDetail.
        return work.getManagedResourceId();
    }

    @Override
    public Processor createDependencies(Work work, Processor processor) {
        LOGGER.info("Creating dependencies for '{}' [{}]",
                processor.getName(),
                processor.getId());
        return persist(processor);
    }

    @Override
    public Processor deleteDependencies(Work work, Processor processor) {
        LOGGER.info("Destroying dependencies for '{}' [{}]",
                processor.getName(),
                processor.getId());
        return persist(processor);
    }
}
