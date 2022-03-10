package com.redhat.service.bridge.manager.workers.resources;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.dao.ConnectorsDAO;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.models.Work;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class ProcessorWorker extends AbstractWorker<Processor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorWorker.class);

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    ConnectorsDAO connectorsDAO;

    @Override
    protected PanacheRepositoryBase<Processor, String> getDao() {
        return processorDAO;
    }

    @Override
    public Processor createDependencies(Work work, Processor processor) {
        LOGGER.info("Creating dependencies for '{}' [{}]",
                processor.getName(),
                processor.getId());

        if (hasZeroConnectors(processor)) {
            LOGGER.debug(
                    "No dependencies required for '{}' [{}]",
                    processor.getName(),
                    processor.getId());
            processor.setDependencyStatus(ManagedResourceStatus.READY);
            return persist(processor);
        }

        // Update Processor's dependency status
        ConnectorEntity dependency = getConnectorEntity(processor);
        processor.setDependencyStatus(dependency.getStatus());

        // If the Connector failed we should mark the Processor as failed too
        if (dependency.getStatus() == ManagedResourceStatus.FAILED) {
            processor.setStatus(ManagedResourceStatus.FAILED);
        }

        persist(processor);

        return processor;
    }

    @Override
    protected boolean isProvisioningComplete(Processor managedResource) {
        //As far as the Worker mechanism is concerned work for a Processor is complete when the dependencies are complete.
        return PROVISIONING_COMPLETED.contains(managedResource.getDependencyStatus());
    }

    @Override
    public Processor deleteDependencies(Work work, Processor processor) {
        LOGGER.info("Destroying dependencies for '{}' [{}]",
                processor.getName(),
                processor.getId());

        if (hasZeroConnectors(processor)) {
            LOGGER.debug("No dependencies required for '{}' [{}]",
                    processor.getName(),
                    processor.getId());
            processor.setDependencyStatus(ManagedResourceStatus.DELETED);
            return persist(processor);
        }

        // Update Processor's dependency status
        ConnectorEntity dependency = getConnectorEntity(processor);
        processor.setDependencyStatus(dependency.getStatus());

        // If the Connector failed we should mark the Processor as failed too
        if (dependency.getStatus() == ManagedResourceStatus.FAILED) {
            processor.setStatus(ManagedResourceStatus.FAILED);
        }

        persist(processor);

        return processor;
    }

    @Override
    protected boolean isDeprovisioningComplete(Processor managedResource) {
        //As far as the Worker mechanism is concerned work for a Processor is complete when the dependencies are complete.
        return DEPROVISIONING_COMPLETED.contains(managedResource.getDependencyStatus());
    }

    protected boolean hasZeroConnectors(Processor processor) {
        return Objects.isNull(getConnectorEntity(processor));
    }

    protected ConnectorEntity getConnectorEntity(Processor processor) {
        return connectorsDAO.findByProcessorId(processor.getId());
    }

}
