package com.redhat.service.smartevents.manager.v2.workers.resources;

import java.util.concurrent.Callable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ManagedResourceV2DAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

@ApplicationScoped
public class ProcessorWorker extends AbstractWorker<Processor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorWorker.class);

    private static final String PROCESSOR_WORKER_CLASSNAME = Processor.class.getName();

    @Inject
    ProcessorDAO processorDAO;

    @Override
    public ManagedResourceV2DAO<Processor> getDao() {
        return processorDAO;
    }

    @Override
    public String getId(Work work) {
        // The ID of the ManagedResource to process is stored directly in the JobDetail.
        return work.getManagedResourceId();
    }

    @Override
    public Processor createDependencies(Work work, Processor processor) {
        LOGGER.info("Setting ControlPlane to Ready for '{}' [{}]",
                processor.getName(),
                processor.getId());

        Callable<Void> createVoidCallable = () -> null;
        execute(DefaultConditions.CP_CONTROL_PLANE_READY_NAME,
                processor,
                createVoidCallable,
                defaultOnResult(),
                defaultOnException());

        return persist(processor);
    }

    @Override
    public Processor deleteDependencies(Work work, Processor processor) {
        LOGGER.info("Setting ControlPlane to Deleted for '{}' [{}]",
                processor.getName(),
                processor.getId());

        Callable<Void> createVoidCallable = () -> null;
        execute(DefaultConditions.CP_CONTROL_PLANE_DELETED_NAME,
                processor,
                createVoidCallable,
                defaultOnResult(),
                defaultOnException());

        return persist(processor);
    }

    @Override
    public boolean accept(Work work) {
        return PROCESSOR_WORKER_CLASSNAME.equals(work.getType());
    }
}
