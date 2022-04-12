package com.redhat.service.rhose.manager.workers.resources;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.rhose.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.rhose.manager.dao.ConnectorsDAO;
import com.redhat.service.rhose.manager.dao.ProcessorDAO;
import com.redhat.service.rhose.manager.models.ConnectorEntity;
import com.redhat.service.rhose.manager.models.Processor;
import com.redhat.service.rhose.manager.models.Work;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class ProcessorWorker extends AbstractWorker<Processor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorWorker.class);

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    ConnectorsDAO connectorsDAO;

    @Inject
    ConnectorWorker connectorWorker;

    @Override
    protected PanacheRepositoryBase<Processor, String> getDao() {
        return processorDAO;
    }

    // This must be equal to the Processor.class.getName()
    @ConsumeEvent(value = "com.redhat.service.bridge.manager.models.Processor", blocking = true)
    public Processor handleWork(Work work) {
        return super.handleWork(work);
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

        // If we have to deploy a Managed Connector, delegate to the ConnectorWorker.
        // The Processor will be provisioned by the Shard when it is in ACCEPTED state *and* Connectors are READY (or null).
        return delegate(work, processor);
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

        return delegate(work, processor);
    }

    @Override
    protected boolean isDeprovisioningComplete(Processor managedResource) {
        //As far as the Worker mechanism is concerned work for a Processor is complete when the dependencies are complete.
        return DEPROVISIONING_COMPLETED.contains(managedResource.getDependencyStatus());
    }

    private Processor delegate(Work work, Processor processor) {
        //Get Processor's Connector for which work needs completing
        final ConnectorEntity connectorEntity = getConnectorEntity(processor);

        //Delegate to the ConnectorWorker however mimic that the Work originated from the Processor.
        Work connectorEntityWork = Work.forDependentResource(connectorEntity, work);
        ConnectorEntity updatedConnectorEntity = connectorWorker.handleWork(connectorEntityWork);
        processor.setDependencyStatus(updatedConnectorEntity.getStatus());

        // If the Connector failed we should mark the Processor as failed too
        if (updatedConnectorEntity.getStatus() == ManagedResourceStatus.FAILED) {
            processor.setStatus(ManagedResourceStatus.FAILED);
        }

        return persist(processor);
    }

    protected boolean hasZeroConnectors(Processor processor) {
        return Objects.isNull(getConnectorEntity(processor));
    }

    protected ConnectorEntity getConnectorEntity(Processor processor) {
        return connectorsDAO.findByProcessorId(processor.getId());
    }

}
