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

    // This must be equal to the Processor.class.getSimpleName()
    @ConsumeEvent(value = "Processor", blocking = true)
    public boolean handleWork(Work work) {
        return super.handleWork(work);
    }

    @Override
    protected Processor runCreateOfDependencies(Work work, Processor processor) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Creating dependencies for '%s' [%s]",
                    processor.getName(),
                    processor.getId()));
        }

        if (hasZeroConnectors(processor)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("No dependencies required for '%s' [%s]",
                        processor.getName(),
                        processor.getId()));
            }
            return setDependencyStatus(processor, ManagedResourceStatus.READY);
        }

        // If we have to deploy a Managed Connector, delegate to the ConnectorWorker.
        // The Processor will be provisioned by the Shard when it is in ACCEPTED state *and* Connectors are READY (or null).
        ConnectorEntity connectorEntity = connectorWorker.createDependencies(work, getConnectorEntity(processor));
        return setDependencyStatus(processor, connectorEntity.getStatus());
    }

    @Override
    protected Processor runDeleteOfDependencies(Work work, Processor processor) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Destroying dependencies for '%s' [%s]",
                    processor.getName(),
                    processor.getId()));
        }

        if (hasZeroConnectors(processor)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("No dependencies required for '%s' [%s]",
                        processor.getName(),
                        processor.getId()));
            }
            return setDependencyStatus(processor, ManagedResourceStatus.DELETED);
        }

        ConnectorEntity connectorEntity = connectorWorker.deleteDependencies(work, getConnectorEntity(processor));
        return setDependencyStatus(processor, connectorEntity.getStatus());
    }

    protected boolean hasZeroConnectors(Processor processor) {
        return Objects.isNull(getConnectorEntity(processor));
    }

    protected ConnectorEntity getConnectorEntity(Processor processor) {
        return connectorsDAO.findByProcessorId(processor.getId());
    }

}
